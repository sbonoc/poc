package com.agnostic.producer.contract

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.agnostic.common.events.OrderCreatedV1
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@Provider("order-event-producer")
@PactFolder("../consumer/pacts")
@ExtendWith(PactVerificationInvocationContextProvider::class)
class ProducerPactVerificationTest {
    private val pactJson = Json { encodeDefaults = true }

    @BeforeEach
    fun before(context: PactVerificationContext) {
        context.target = MessageTestTarget()
    }

    @TestTemplate
    fun verifyPact(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @PactVerifyProvider("an order created v1 event")
    fun orderCreatedMessage(): String {
        val event = OrderCreatedV1(id = "ORD-300", amount = BigDecimal("55.10"))
        return pactJson.encodeToString(event)
    }
}
