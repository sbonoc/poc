package com.agnostic.consumer.infrastructure

import com.agnostic.common.serialization.JsonSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OrderEventParserTest {
    private val parser = OrderEventParser(JsonSupport.default)

    @Test
    fun `parses cloudevent payload`() {
        val payload =
            """
            {
              "id": "event-1",
              "source": "producer",
              "specversion": "1.0",
              "type": "order.created.v1",
              "data": {
                "id": "ORD-1",
                "amount": 20.0,
                "eventVersion": "v1"
              }
            }
            """.trimIndent()

        val event = parser.parse(payload)

        assertThat(event.id).isEqualTo("ORD-1")
        assertThat(event.amount).isEqualByComparingTo("20.0")
    }

    @Test
    fun `parses raw order payload`() {
        val payload = """{"id":"ORD-2","amount":30.0,"eventVersion":"v1"}"""

        val event = parser.parse(payload)

        assertThat(event.id).isEqualTo("ORD-2")
    }

    @Test
    fun `fails for invalid payload`() {
        val payload = """{"bad":"payload"}"""

        assertThatThrownBy { parser.parse(payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("payload")
    }
}
