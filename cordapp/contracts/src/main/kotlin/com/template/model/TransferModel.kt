package com.template.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class TransferModel(
    val orgFrom : Party,
    val accountFrom : String,
    val accountFromName : String,
    val orgTo : Party,
    val accountTo : String,
    val accountToName : String,
    val createTime : Instant,
    val amount : Double
)

/*
@CordaSerializable
enum class TransferStatusEnum {
    PENDING, SHARED, CLOSED
}
*/