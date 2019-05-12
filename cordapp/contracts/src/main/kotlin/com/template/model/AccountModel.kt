package com.template.model

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class AccountModel(
    val org : Party,
    val orgName : String,
    val createTime : Instant,
    val name : String,
    val document : String,
    val balance : Double
)