package com.agnostic.producer.dto

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PublishOrderRequestTest {
    @Test
    fun `validate rejects blank id`() {
        val request = PublishOrderRequest(id = " ", amount = BigDecimal("10.0"))

        assertThatThrownBy { request.validate() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("id")
    }

    @Test
    fun `validate rejects non positive amount`() {
        val request = PublishOrderRequest(id = "ORD-10", amount = BigDecimal.ZERO)

        assertThatThrownBy { request.validate() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("amount")
    }
}
