package com.agnostic.consumerspringboot;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ConsumerController {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerController.class);

    private final ConsumerService consumerService;

    ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @GetMapping("/dapr/subscribe")
    ResponseEntity<List<Map<String, String>>> subscribe() {
        logger.debug("received dapr subscription discovery request");
        return consumerService.subscriptions();
    }

    @PostMapping("${app.subscription.route:/orders}")
    ResponseEntity<Void> consume(@RequestBody JsonNode payload) {
        if (logger.isDebugEnabled()) {
            int payloadSize = payload == null ? 0 : payload.toString().length();
            logger.debug("received consume request payloadSize={}", payloadSize);
        }
        return consumerService.consume(payload);
    }
}
