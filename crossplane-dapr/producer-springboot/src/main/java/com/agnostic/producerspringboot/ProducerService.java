package com.agnostic.producerspringboot;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
class ProducerService {
    private static final Logger logger = LoggerFactory.getLogger(ProducerService.class);

    private final RestTemplate restTemplate;
    private final ProducerConfig config;
    private final Counter publishRequests;
    private final Counter publishErrors;
    private final Counter publishedEvents;

    ProducerService(RestTemplate restTemplate, ProducerConfig config, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.publishRequests = meterRegistry.counter("orders.publish.requests");
        this.publishErrors = meterRegistry.counter("orders.publish.errors");
        this.publishedEvents = meterRegistry.counter("orders.published");
    }

    ResponseEntity<Map<String, String>> publish(PublishOrderRequest request) {
        publishRequests.increment();
        logger.debug("processing publish request orderId={} amount={}", request.id(), request.amount());
        Map<String, Object> event =
            Map.of("id", request.id(), "amount", request.amount(), "eventVersion", "v1");

        String url =
            "http://localhost:%d/v1.0/publish/%s/%s"
                .formatted(config.httpPort(), config.pubsubName(), config.topicName());
        logger.debug("publishing order event orderId={} url={}", request.id(), url);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, new HttpEntity<>(event), Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                publishErrors.increment();
                logger.warn(
                    "publish endpoint returned non-success orderId={} status={}",
                    request.id(),
                    response.getStatusCode());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "publish endpoint returned non-success"));
            }
        } catch (RestClientException ex) {
            publishErrors.increment();
            logger.error("failed to publish order event orderId={}", request.id(), ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "failed to publish event"));
        }

        publishedEvents.increment();
        logger.info(
            "published order event orderId={} version=v1 pubsub={} topic={}",
            request.id(),
            config.pubsubName(),
            config.topicName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of("status", "accepted", "orderId", request.id()));
    }
}
