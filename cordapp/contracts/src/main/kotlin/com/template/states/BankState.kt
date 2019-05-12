package com.template.states

import com.template.contracts.BankContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import com.template.model.BankModel
import com.template.schema.BankSchemaV1
import net.corda.core.contracts.BelongsToContract

@BelongsToContract(BankContract::class)
data class BankState(val bank: BankModel,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(bank.org)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {

        return when (schema) {
            is BankSchemaV1 -> BankSchemaV1.PersistentAccount(
                    this.bank.bankId
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }

    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(BankSchemaV1)
    }

}