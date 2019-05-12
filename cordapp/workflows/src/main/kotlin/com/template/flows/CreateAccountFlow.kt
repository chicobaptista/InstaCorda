package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AccountContract
import com.template.model.AccountModel
import com.template.states.AccountState
//import com.template.model.AccountStatusEnum
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant
import java.util.*

/*
flow start CreateDebitFlow uid: uid1, pubkey: pubkey1, currency: brl1, hashProfile: hash1, balance: 1000

run vaultQuery contractStateType: tech.bluchain.blucard.state.DebitState
* */

object AccountFlow {

    @InitiatingFlow
    @StartableByRPC
    class CreateAccount(
            val name: String,
            val document: String,
            val balance: Double
    ) : FlowLogic<SignedTransaction>() {

        companion object {

            object INITIALISING : ProgressTracker.Step("Initialising")
            object BUILDING : ProgressTracker.Step("Building")
            object SIGNING : ProgressTracker.Step("Signing")

            object FINALISING : ProgressTracker.Step("Finalising") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    INITIALISING,
                    BUILDING,
                    SIGNING,
                    FINALISING
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            logger.error("FLOW 1")


            progressTracker.currentStep = INITIALISING

            // Step 1 - Verifies whether the account requested already exists in this Bank
            val accountList = getAccountStateByDocument(document)
            if (accountList.isNotEmpty()) {
                // If the account already exist throw an error
                throw Exception("\n" + "This account already exists")
            }

            // Step 2 - Define a new account initial data
            val account = AccountModel(ourIdentity, ourIdentity.name.organisation, Instant.now(), name, document, balance)

            // Step 3 - Creates a new state for the account
            val accountState = AccountState(account)

            // Step 4 - Create the command list for contract validation
            val txCommand = Command(
                    AccountContract.Commands.CreateAccount(),
                    accountState.participants.map { it.owningKey }
            )

            progressTracker.currentStep = BUILDING

            // Step 5 - Define a notary for this transaction
            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            // Step 6 - Build and validating the transaction
            val txBuilder = TransactionBuilder(notary)
                    .addCommand(txCommand)
                    .addOutputState(accountState, AccountContract::class.java.canonicalName)

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING

            // Step 7 - The Bank (Org) sing the transaction
            val signedTx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)

            progressTracker.currentStep = FINALISING

            // Step 8 - Finalising the single party flow
            return subFlow(FinalityFlow(signedTx, emptyList()))

        }

        private fun getAccountStateByDocument(id : String) : List<StateAndRef<AccountState>> {

            val uuid = UUID.fromString(id)

            val query = QueryCriteria.LinearStateQueryCriteria( uuid = listOf(uuid) )

            return serviceHub.vaultService.queryBy<AccountState>(query).states


        }

    }

}