package com.agnostic.consumerspringboot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConsumerConfig.class)
class ConsumerRuntimeConfig {}
