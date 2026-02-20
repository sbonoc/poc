package com.agnostic.consumerspringboot;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ConsumerController {
    private final ConsumerService consumerService;

    ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @GetMapping("/dapr/subscribe")
    ResponseEntity<List<Map<String, String>>> subscribe() {
        return consumerService.subscriptions();
    }

    @PostMapping("${app.subscription.route:/orders}")
    ResponseEntity<Void> consume(@RequestBody JsonNode payload) {
        return consumerService.consume(payload);
    }
}
