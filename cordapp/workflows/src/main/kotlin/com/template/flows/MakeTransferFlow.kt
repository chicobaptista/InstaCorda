package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap
import com.template.contracts.AccountContract
import com.template.contracts.TransferContract
import com.template.model.TransferModel
import com.template.states.TransferState
import java.time.Instant

// flow start TransferFlow accountFromId: 37144a4a-c591-4cc0-bd16-d7d2d6f72971, orgTo: OrgB, to: f120697c-b454-491e-bda4-bbd61675054d, amount: 100.00
// run vaultQuery contractStateType: tech.bluchain.democordanodes.state.TransferState
object Transfer {

    @InitiatingFlow
    @StartableByRPC
    class TransferFlow(
        val accountFromId: String,
        val orgTo: Party,
        val to: String,
        val amount: Double
    ) : BaseFlow() {

        companion object {

            object INITIALISING : ProgressTracker.Step("Inicializando")
            object BUILDING : ProgressTracker.Step("Construindo")
            object SIGNING : ProgressTracker.Step("Assinando")
            object PART_SIGNING : ProgressTracker.Step("Obtendo assinatura das partes") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING : ProgressTracker.Step("Finalizando") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                INITIALISING,
                BUILDING,
                SIGNING,
                PART_SIGNING,
                FINALISING
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {

            // define progresso
            progressTracker.currentStep = INITIALISING

            // criando uma sessao com o nó de destino
            val sessionTo = initiateFlow(this.orgTo)

            // Obtendo o nome da conta de destino
            val packNameTo: UntrustworthyData<String> = sessionTo.sendAndReceive<String>(this.to)
            val nameTo: String = packNameTo.unwrap { data -> data }

            // obtém a conta de origem para ser consumida
            val oldAccountStateAndRefList = getAccountStateById(this.accountFromId)
            if (oldAccountStateAndRefList.isEmpty()) {
                throw Exception("Conta de origem não encontrada")
            }
            val oldAccountStateAndRef = oldAccountStateAndRefList.single()
            val oldAccountState = oldAccountStateAndRef.state.data

            val newBalance = oldAccountState.account.balance - this.amount

            // criando uma nova conta
            val newAccount = oldAccountState.account.copy(
                balance = newBalance
            )

            // criando o novo state
            val newAccountState = oldAccountState.copy(
                account = newAccount
            )

            // define uma conta
            val transfer = TransferModel(ourIdentity, this.accountFromId, oldAccountState.account.name, this.orgTo, this.to, nameTo, Instant.now(), this.amount)

            // cria o state
            val transferState = TransferState(transfer)

            // criando o command para validação do transfer contract
            val transferTxCommand = Command(
                TransferContract.Commands.CreateTransfer(),
                transferState.participants.map { it.owningKey }
            )

            // criando o command para validação do account contract
            val accountTxCommand = Command(
                AccountContract.Commands.TransferAccountFrom(),
                oldAccountState.participants.map { it.owningKey }
            )

            progressTracker.currentStep = BUILDING

            // criando a transação e validando
            val txBuilder = TransactionBuilder(notary)
                .addCommand(accountTxCommand)
                .addCommand(transferTxCommand)
                .addInputState(oldAccountStateAndRef)
                .addOutputState(newAccountState, AccountContract::class.java.canonicalName)
                .addOutputState(transferState, TransferContract::class.java.canonicalName)

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING

            // banco assinando a transação
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)

            progressTracker.currentStep = PART_SIGNING

            val session = listOf(this.orgTo).toSet().map { sessionTo }
            val signedTx = subFlow(CollectSignaturesFlow(partSignedTx, session, PART_SIGNING.childProgressTracker()))

            progressTracker.currentStep = FINALISING

            val finalityFlow = subFlow(FinalityFlow(signedTx, FINALISING.childProgressTracker()))


            val pack : UntrustworthyData<SignedTransaction> = sessionTo.sendAndReceive<SignedTransaction>(signedTx)

            val partSignedTxRequired = pack.unwrap {data -> data }

            val mySignedTx: SignedTransaction = serviceHub.addSignature(partSignedTxRequired)

            sessionTo.sendAndReceive<SignedTransaction>(mySignedTx)

            return finalityFlow

        }
    }

    @InitiatedBy(TransferFlow::class)
    class OrgToResponderFlow(val partyFlow: FlowSession) : BaseFlow() {

        companion object {

            object INITIALISING : ProgressTracker.Step("Inicializando destino")
            object BUILDING : ProgressTracker.Step("Construindo destino")
            object SIGNING : ProgressTracker.Step("Assinando destino")
            object PART_SIGNING : ProgressTracker.Step("Obtendo assinatura das partes origem") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING : ProgressTracker.Step("Finalizando destino") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                INITIALISING,
                BUILDING,
                SIGNING,
                PART_SIGNING,
                FINALISING
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {


            /*
            * ################################################################################################
            * FLUXO 1 - PRINCIPAL PEDE O NOME DA CONTA
            * ################################################################################################
            * */

            // obtém o parametro linearId de destino para encontrar a conta
            val toId: String = partyFlow.receive<String>().unwrap { data -> data }

            // procura pela conta
            val oldAccountStateAndRefList = getAccountStateById(toId)
            if (oldAccountStateAndRefList.isEmpty()) {
                throw Exception("Conta de destino não encontrada")
            }

            // obtém a conta
            val oldAccountStateAndRef = oldAccountStateAndRefList.single()
            val oldAccountState = oldAccountStateAndRef.state.data

            // responde para o fluxo principal o nome da conta
            partyFlow.send(oldAccountState.account.name)

            /*
            * ################################################################################################
            * FLUXO 2 - PRINCIPAL PEDE A ASSINATURA DA TRANSACAO
            * ################################################################################################
            * */

            if (ourIdentity != partyFlow.counterparty) {

                // assina a transação principal
                val signTransactionFlow = object : SignTransactionFlow(partyFlow) {

                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        "Conta de destino não encontrada" using (oldAccountStateAndRefList.isNotEmpty())
                    }
                }

                // responde com a assinatura
                subFlow(signTransactionFlow)
            }

            /*
            * ################################################################################################
            * FLUXO 3 - PRINCIPAL PEDE PARA ALTERAR O SALDO DE DESTINO
            * ################################################################################################
            * */

            val signTx: SignedTransaction = partyFlow.receive<SignedTransaction>().unwrap { data -> data }

            progressTracker.currentStep = OrgToResponderFlow.Companion.INITIALISING

            // obtém a transferencia compartilhada
            val transferState = signTx.tx.outputsOfType<TransferState>().single()

            val oldTransferStateAndRefList = getTransferStateById(transferState.linearId.toString())
            if (oldTransferStateAndRefList.isEmpty()) {
                throw Exception("Solicitação de transferência não encontrada")
            }

            // obtém a transferencia
            val oldTransferStateAndRef = oldTransferStateAndRefList.single()

            val newBalance = oldAccountState.account.balance + transferState.transfer.amount

            // criando uma nova conta
            val newAccount = oldAccountState.account.copy(
                balance = newBalance
            )

            // criando o novo state para conta
            val newAccountState = oldAccountState.copy(
                account = newAccount
            )

            // criando o command para validação do transfer contract
            val transferTxCommand = Command(
                TransferContract.Commands.FinalisingTransfer(),
                transferState.participants.map { it.owningKey }
            )

            // criando o command para validação do account contract
            val accountTxCommand = Command(
                AccountContract.Commands.TransferAccountTo(),
                oldAccountState.participants.map { it.owningKey }
            )

            progressTracker.currentStep = OrgToResponderFlow.Companion.BUILDING

            // criando a transação e validando
            val txBuilder = TransactionBuilder(notary)
                .addCommand(transferTxCommand)
                .addCommand(accountTxCommand)
                .addInputState(oldTransferStateAndRef)
                .addInputState(oldAccountStateAndRef)
                .addOutputState(newAccountState, AccountContract::class.java.canonicalName)

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = OrgToResponderFlow.Companion.SIGNING

            // assinando a TX
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)

            progressTracker.currentStep = OrgToResponderFlow.Companion.PART_SIGNING

            // enviando a TX para a parte de origem assinar
            val pack : UntrustworthyData<SignedTransaction> = partyFlow.sendAndReceive<SignedTransaction>(partSignedTx)
            val signedTx = pack.unwrap {data -> data }

            progressTracker.currentStep = OrgToResponderFlow.Companion.FINALISING

            // finalizando e gravando os states
            val finalityFlow = subFlow(
                FinalityFlow(
                    signedTx,
                    OrgToResponderFlow.Companion.FINALISING.childProgressTracker()
                )
            )

            // respondendo para a parte inicial para finalizar
            partyFlow.send(finalityFlow)

            return finalityFlow;

        }
    }

}
