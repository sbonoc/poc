package com.agnostic.producerspringboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dapr")
record ProducerConfig(String pubsubName, String topicName, int httpPort) {}
