package com.agnostic.producerspringboot;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/health/live")
    ResponseEntity<Map<String, String>> live() {
        logger.debug("liveness probe requested");
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/health/ready")
    ResponseEntity<Map<String, String>> ready() {
        logger.debug("readiness probe requested");
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

