package com.template.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class BankModel(
    val org : Party,
    val orgName : String,
    val createTime : Instant,
    val bankName : String,
    val bankId : String,
    val balance : Double
)