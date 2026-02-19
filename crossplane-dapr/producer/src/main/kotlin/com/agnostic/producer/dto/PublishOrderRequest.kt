package com.agnostic.producer.dto

import com.agnostic.common.events.OrderCreatedV1
import com.agnostic.common.serialization.BigDecimalAsNumberSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PublishOrderRequest(
    val id: String,
    @Serializable(with = BigDecimalAsNumberSerializer::class)
    val amount: BigDecimal,
) {
    fun validate() {
        require(id.isNotBlank()) { "id must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be greater than 0" }
    }

    fun toEvent(): OrderCreatedV1 = OrderCreatedV1(id = id, amount = amount)
}
