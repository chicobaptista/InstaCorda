package com.template.flows

import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import com.template.schema.AccountSchemaV1
import com.template.schema.TransferSchemaV1
import com.template.states.AccountState
import com.template.states.TransferState
import java.io.File
import java.util.*
import javax.ws.rs.core.Response

abstract class BaseFlow : FlowLogic<SignedTransaction>() {

    val notary get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull() ?: throw FlowException("Notary not found")

    fun getAccountStateById(id : String) : List<StateAndRef<AccountState>> {

        val uuid = UUID.fromString(id)

        val query = QueryCriteria.LinearStateQueryCriteria( uuid = listOf(uuid) )

        return serviceHub.vaultService.queryBy<AccountState>(query).states

    }

    fun getAccountStateByDocument(document : String) : List<StateAndRef<AccountState>> {

        val indexDocument = AccountSchemaV1.PersistentAccount::document.equal(document)
        val criteria = QueryCriteria.VaultCustomQueryCriteria(expression = indexDocument)

        return serviceHub.vaultService.queryBy<AccountState>(criteria).states
    }

    fun getTransferStateById(id : String) : List<StateAndRef<TransferState>> {

        val uuid = UUID.fromString(id)

        val query = QueryCriteria.LinearStateQueryCriteria( uuid = listOf(uuid) )

        return serviceHub.vaultService.queryBy<TransferState>(query).states

    }

    fun getFlow(key : String) : String? {

        var result : String = "NOT_FOUND"

        try {
            val fis = File("node.properties").inputStream()
            val resource = PropertyResourceBundle(fis)
            result = resource.getString(key)
        } catch (e : Exception){

        }

        return result
    }

}