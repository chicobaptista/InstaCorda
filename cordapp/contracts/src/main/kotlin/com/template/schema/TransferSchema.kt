package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.*

/**
 * The family of schemas for TransferState.
 */
object TransferSchema

/**
 * An TransferState schema.
 */
object TransferSchemaV1 : MappedSchema(
    schemaFamily = TransferSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentTransfer::class.java)) {
    @Entity
    @Table(name = "transfer_states", indexes = [Index(name = "acct_from_idx", columnList="account_from"),Index(name = "acct_to_idx", columnList="account_to")])
    class PersistentTransfer(

        @Column(name = "account_from")
        var accountFrom: String,

        @Column(name = "account_to")
        var accountTo: String

    ) : PersistentState() {
        constructor(): this("","")
    }

}