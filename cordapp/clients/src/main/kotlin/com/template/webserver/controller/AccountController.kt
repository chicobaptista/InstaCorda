package com.template.webserver.controller

import com.template.model.requests.AccountRequestModel
import com.template.schema.AccountSchemaV1
import com.template.states.AccountState
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.Builder.equal
import com.template.webserver.NodeRPCConnection
import net.corda.core.node.services.Vault
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.*
import net.corda.core.node.services.vault.QueryCriteria

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A CorDapp-agnostic controller that exposes standard endpoints.
 */

@RestController
@RequestMapping("/account") // The paths for GET and POST requests are relative to this base path.
class AccountController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    @CrossOrigin
    @PostMapping(value = "/create", produces = arrayOf(MediaType.APPLICATION_JSON), consumes = arrayOf(MediaType.APPLICATION_JSON))
    fun createAccountCreateRequestModel(@RequestBody data: AccountRequestModel): Response {

        val indexUid     = AccountSchemaV1.PersistentAccount::document.equal(data.document)
        val criteria = QueryCriteria.VaultCustomQueryCriteria(expression = indexUid)
        val states = proxy.vaultQueryBy<AccountState>(criteria).states

        if (states.isNotEmpty()){
            return Response.status(Response.Status.BAD_REQUEST).entity("UID já existe nesta Org.\n").build()
        }

        if (data.balance < 0 ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Valor não deve ser negativo.\n").build()
        }

        return try {

            val signedTx = this.proxy.startTrackedFlow(com.template.flows.AccountFlow::CreateAccount, data.name, data.document, data.balance).returnValue.getOrThrow()
            Response.status(Response.Status.CREATED).entity( signedTx.tx.outputs.single() ).build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            logger.error(ex.toString())
            Response.status(Response.Status.BAD_REQUEST).entity(ex.message).build()
        }
    }


    @GetMapping(value = "/states", produces = arrayOf(MediaType.APPLICATION_JSON))
    private fun states(): Response {
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)

        val result = proxy.vaultQueryBy<AccountState>(criteria = criteria).states

        return Response.status(Response.Status.CREATED).entity( result ).build()
    }


    @GetMapping(value = "/client/{document}", produces = arrayOf(MediaType.APPLICATION_JSON))
    private fun getUserById(@PathVariable document: String) : Response {

        val indexUid = AccountSchemaV1.PersistentAccount::document.equal(document)
        val criteria = QueryCriteria.VaultCustomQueryCriteria(expression = indexUid)

        val states = proxy.vaultQueryBy<AccountState>(criteria).states

        return if (states.isNotEmpty()){

            Response.status(Response.Status.CREATED).entity(states).build()

        } else {

            Response.status(Response.Status.NOT_FOUND).entity("Balance not found").build()
        }

    }

    @GetMapping(value = "/history/{document}", produces = arrayOf(MediaType.APPLICATION_JSON))
    private fun getHistoryById(@PathVariable document: String) : Response {

        val indexUid = AccountSchemaV1.PersistentAccount::document.equal(document)
        val criteria = QueryCriteria.VaultCustomQueryCriteria(expression = indexUid, status = Vault.StateStatus.CONSUMED)

        val states = proxy.vaultQueryBy<AccountState>(criteria).states

        return if (states.isNotEmpty()){

            Response.status(Response.Status.CREATED).entity(states).build()

        } else {

            Response.status(Response.Status.NOT_FOUND).entity("History not Found").build()
        }

    }
}