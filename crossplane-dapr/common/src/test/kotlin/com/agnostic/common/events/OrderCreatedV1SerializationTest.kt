package com.agnostic.common.events

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderCreatedV1SerializationTest {
    @Test
    fun `deserializes with default event version`() {
        val event = OrderCreatedV1(id = "ORD-101", amount = BigDecimal("42.50"))

        val json = Json.encodeToString(OrderCreatedV1.serializer(), event)
        val decoded = Json.decodeFromString(OrderCreatedV1.serializer(), json)

        assertThat(decoded.eventVersion).isEqualTo(OrderCreatedV1.EVENT_VERSION)
        assertThat(decoded.amount).isEqualByComparingTo("42.50")
    }
}
