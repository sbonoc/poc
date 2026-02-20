package com.agnostic.consumerspringboot;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
class ConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    private final ConsumerConfig config;
    private final Counter consumeRequests;
    private final Counter consumeErrors;
    private final Counter consumedEvents;

    ConsumerService(ConsumerConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.consumeRequests = meterRegistry.counter("orders.consume.requests");
        this.consumeErrors = meterRegistry.counter("orders.consume.errors");
        this.consumedEvents = meterRegistry.counter("orders.consumed");
    }

    ResponseEntity<List<Map<String, String>>> subscriptions() {
        return ResponseEntity.ok(
            List.of(
                Map.of(
                    "pubsubname", config.pubsubName(),
                    "topic", config.topicName(),
                    "route", config.normalizedRoute())));
    }

    ResponseEntity<Void> consume(JsonNode payload) {
        consumeRequests.increment();

        JsonNode eventNode = payload.has("data") ? payload.get("data") : payload;
        JsonNode idNode = eventNode.get("id");
        if (idNode == null || idNode.asText().isBlank()) {
            consumeErrors.increment();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String eventVersion = eventNode.has("eventVersion") ? eventNode.get("eventVersion").asText("v1") : "v1";
        consumedEvents.increment();
        logger.info("consumed order event route={} id={} version={}", config.normalizedRoute(), idNode.asText(), eventVersion);
        return ResponseEntity.ok().build();
    }
}
