package com.agnostic.consumerspringboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subscription")
record ConsumerConfig(String pubsubName, String topicName, String route) {
    String normalizedRoute() {
        if (route == null || route.isBlank()) {
            return "/orders";
        }
        return route.startsWith("/") ? route : "/" + route;
    }
}
