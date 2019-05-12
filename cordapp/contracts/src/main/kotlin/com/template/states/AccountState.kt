package com.template.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import com.template.model.AccountModel
import com.template.schema.AccountSchemaV1

data class AccountState(val account: AccountModel,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(account.org)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {

        return when (schema) {
            is AccountSchemaV1 -> AccountSchemaV1.PersistentAccount(
                    this.account.document
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }

    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(AccountSchemaV1)
    }

}