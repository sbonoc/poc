package com.agnostic.consumerspringboot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ConsumerServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void subscriptionsReflectConfiguredRoute() {
        ConsumerConfig config = new ConsumerConfig("order-pubsub", "orders", "orders");
        ConsumerService service = new ConsumerService(config, new SimpleMeterRegistry());

        ResponseEntity<List<Map<String, String>>> response = service.subscriptions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst())
            .containsEntry("pubsubname", "order-pubsub")
            .containsEntry("topic", "orders")
            .containsEntry("route", "/orders");
    }

    @Test
    void consumeRejectsMissingId() throws Exception {
        ConsumerConfig config = new ConsumerConfig("order-pubsub", "orders", "/orders");
        ConsumerService service = new ConsumerService(config, new SimpleMeterRegistry());

        ResponseEntity<Void> response = service.consume(objectMapper.readTree("{\"data\":{}}"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
