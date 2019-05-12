package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import com.template.states.BankState
import com.template.states.TransferState

class BankContract : Contract {


    override fun verify(tx: LedgerTransaction) {

        val command = tx.commandsOfType<Commands>().single()

        when (command.value) {
            is Commands.CreateBank -> verifyCreateBank(tx)
            is Commands.TransferBankFrom -> verifyTransferBankFrom(tx)
            is Commands.TransferBankTo -> verifyTransferBankTo(tx)
            else -> throw IllegalArgumentException("Command not found")
        }

    }

    // validação para criação de uma nova conta
    private fun verifyCreateBank(tx: LedgerTransaction){

        requireThat {
            "Para criação de conta nenhum estado deve ser consumido" using (tx.inputs.isEmpty())
            "Apenas uma conta deve ser criada" using (tx.outputs.size == 1)
            val out = tx.outputsOfType<BankState>().single()
            "Não é permitido valor negativo" using (out.bank.balance >= 0.00)
//            "Somente o Banco Central pode criar a conta de um banco" using (out.account.orgName == "CentralBank")

        }

    }

    // validação de conta de origem para transferência
    private fun verifyTransferBankFrom(tx: LedgerTransaction){

        requireThat {
            "Para criar uma transferência apenas uma conta deve ser consumida" using (tx.inputs.size == 1)
            "Para criar uma transferência deve gerar uma nova conta e uma transferência" using (tx.outputs.size == 2)
            val inAcct = tx.inputsOfType<BankState>().single()
            val outTransfer = tx.outputsOfType<TransferState>().single()
            "Saldo insuficiente para realizar a transferência" using (inAcct.bank.balance >= outTransfer.transfer.amount)
        }

    }

    // validação da conta de destino
    private fun verifyTransferBankTo(tx: LedgerTransaction){

        requireThat {
            "Para receber uma transferência deve ser consumida uma conta e uma transferência" using (tx.inputs.size == 2)
            "Para receber uma transferência deve gerar apenas uma nova conta" using (tx.outputs.size == 1)
        }

    }


    interface Commands : CommandData {
        class CreateBank : Commands
        class TransferBankFrom : Commands
        class TransferBankTo : Commands
    }

}