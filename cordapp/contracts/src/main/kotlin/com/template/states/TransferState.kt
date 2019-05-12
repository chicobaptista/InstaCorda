package com.template.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import com.template.model.TransferModel
import com.template.schema.TransferSchemaV1

data class TransferState(val transfer: TransferModel,
                         override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(transfer.orgFrom, transfer.orgTo)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {

        return when (schema) {
            is TransferSchemaV1 -> TransferSchemaV1.PersistentTransfer(
                this.transfer.accountFrom,
                this.transfer.accountTo
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }

    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(TransferSchemaV1)
    }

}