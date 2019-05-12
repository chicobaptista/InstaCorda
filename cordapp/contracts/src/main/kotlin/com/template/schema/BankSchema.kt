package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

/**
 * The family of schemas for AccountState.
 */
object BankSchema

/**
 * An AccountState schema.
 */
object BankSchemaV1 : MappedSchema(
    schemaFamily = BankSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentAccount::class.java)) {
    @Entity
    @Table(name = "bank_states", indexes = [Index(name = "bankId_idx", columnList="bankId")])
    class PersistentAccount(

        @Column(name = "bankId")
        var bankId: String

    ) : PersistentState() {
        constructor(): this("")
    }

}