package com.agnostic.producerspringboot;

import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProducerController {
    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    private final ProducerService producerService;

    ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping("/publish")
    ResponseEntity<Map<String, String>> publish(@Valid @RequestBody PublishOrderRequest request) {
        logger.debug("received publish request orderId={} amount={}", request.id(), request.amount());
        return producerService.publish(request);
    }
}
