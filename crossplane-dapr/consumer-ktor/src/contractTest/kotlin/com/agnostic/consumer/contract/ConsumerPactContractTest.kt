package com.agnostic.consumer.contract

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.consumer.junit5.ProviderType
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import com.agnostic.common.serialization.JsonSupport
import com.agnostic.consumer.infrastructure.OrderEventParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "order-event-producer", providerType = ProviderType.ASYNCH)
class ConsumerPactContractTest {
    private val parser = OrderEventParser(JsonSupport.default)

    @Pact(consumer = "order-event-consumer", provider = "order-event-producer")
    fun orderCreatedPact(builder: MessagePactBuilder): V4Pact =
        builder
            .expectsToReceive("an order created v1 event")
            .withMetadata(mapOf("contentType" to "application/json"))
            .withContent(
                PactDslJsonBody()
                    .stringValue("id", "ORD-300")
                    .numberType("amount", 55.1)
                    .stringValue("eventVersion", "v1"),
            ).toPact(V4Pact::class.java)

    @Test
    @PactTestFor(pactMethod = "orderCreatedPact")
    fun `consumer can parse message defined by pact`(messages: List<V4Interaction.AsynchronousMessage>) {
        val payload = requireNotNull(messages.first().contentsAsString()) { "pact message payload must not be null" }
        val event = parser.parse(payload)

        assertThat(event.id).isEqualTo("ORD-300")
        assertThat(event.eventVersion).isEqualTo("v1")
    }
}
