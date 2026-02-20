package com.agnostic.producerspringboot;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProducerController {
    private final ProducerService producerService;

    ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping("/publish")
    ResponseEntity<Map<String, String>> publish(@Valid @RequestBody PublishOrderRequest request) {
        return producerService.publish(request);
    }
}
