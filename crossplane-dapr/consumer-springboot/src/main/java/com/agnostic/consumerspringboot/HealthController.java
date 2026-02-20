package com.agnostic.consumerspringboot;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {
    @GetMapping("/health/live")
    ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/health/ready")
    ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
