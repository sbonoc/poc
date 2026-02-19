package com.agnostic.common.events

import com.agnostic.common.serialization.BigDecimalAsNumberSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OrderCreatedV1(
    val id: String,
    @Serializable(with = BigDecimalAsNumberSerializer::class)
    val amount: BigDecimal,
    @SerialName("eventVersion")
    val eventVersion: String = EVENT_VERSION,
) {
    companion object {
        const val EVENT_VERSION = "v1"
    }
}
