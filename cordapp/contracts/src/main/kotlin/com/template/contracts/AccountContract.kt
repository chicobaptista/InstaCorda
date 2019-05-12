package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import com.template.states.AccountState
import com.template.states.TransferState

class AccountContract : Contract {

    /*
    companion object {
        @JvmStatic
        val ACCOUNT_CONTRACT_ID = "com.rtm.poc.contract.AccountContract"
    }
    */

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commandsOfType<Commands>().single()

        when (command.value) {
            is Commands.CreateAccount -> verifyCreateAccount(tx)
            is Commands.TransferAccountFrom -> verifyTransferAccountFrom(tx)
            is Commands.TransferAccountTo -> verifyTransferAccountTo(tx)
            else -> throw IllegalArgumentException("Command not found")
        }

    }

    // validação para criação de uma nova conta
    private fun verifyCreateAccount(tx: LedgerTransaction){

        requireThat {
            "Para criação de conta nenhum estado deve ser consumido" using (tx.inputs.isEmpty())
            "Apenas uma conta deve ser criada" using (tx.outputs.size == 1)
            val out = tx.outputsOfType<AccountState>().single()
            "Não é permitido valor negativo" using (out.account.balance >= 0.00)
        }

    }

    // validação de conta de origem para transferência
    private fun verifyTransferAccountFrom(tx: LedgerTransaction){

        requireThat {
            "Para criar uma transferência apenas uma conta deve ser consumida" using (tx.inputs.size == 1)
            "Para criar uma transferência deve gerar uma nova conta e uma transferência" using (tx.outputs.size == 2)
            val inAcct = tx.inputsOfType<AccountState>().single()
            val outTransfer = tx.outputsOfType<TransferState>().single()
            "Saldo insuficiente para realizar a transferência" using (inAcct.account.balance >= outTransfer.transfer.amount)
        }

    }

    // validação da conta de destino
    private fun verifyTransferAccountTo(tx: LedgerTransaction){

        requireThat {
            "Para receber uma transferência deve ser consumida uma conta e uma transferência" using (tx.inputs.size == 2)
            "Para receber uma transferência deve gerar apenas uma nova conta" using (tx.outputs.size == 1)
        }

    }


    interface Commands : CommandData {
        class CreateAccount : Commands
        class TransferAccountFrom : Commands
        class TransferAccountTo : Commands
    }

}