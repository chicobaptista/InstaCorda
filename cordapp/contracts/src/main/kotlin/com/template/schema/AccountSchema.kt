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
object AccountSchema

/**
 * An AccountState schema.
 */
object AccountSchemaV1 : MappedSchema(
    schemaFamily = AccountSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentAccount::class.java)) {
    @Entity
    @Table(name = "account_states", indexes = [Index(name = "document_idx", columnList="document")])
    class PersistentAccount(

        @Column(name = "document")
        var document: String

    ) : PersistentState() {
        constructor(): this("")
    }

}