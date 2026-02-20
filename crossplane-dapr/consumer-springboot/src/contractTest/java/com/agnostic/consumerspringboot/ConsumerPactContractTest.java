package com.agnostic.consumerspringboot;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order-event-producer-springboot", providerType = ProviderType.ASYNCH)
class ConsumerPactContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Pact(consumer = "order-event-consumer-springboot", provider = "order-event-producer-springboot")
    V4Pact orderCreatedMessagePact(MessagePactBuilder builder) {
        return builder
            .expectsToReceive("an order created v1 event from producer-springboot")
            .withMetadata(java.util.Map.of("contentType", "application/json"))
            .withContent(
                new PactDslJsonBody()
                    .stringValue("id", "ORD-SB-300")
                    .numberType("amount", 55.1)
                    .stringValue("eventVersion", "v1"))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "orderCreatedMessagePact")
    void consumerAcceptsMessageDefinedByPact(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        String payload = messages.getFirst().contentsAsString();
        ConsumerConfig config = new ConsumerConfig("order-pubsub", "orders", "/orders");
        ConsumerService service = new ConsumerService(config, new SimpleMeterRegistry());

        var response = service.consume(objectMapper.readTree(payload));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
