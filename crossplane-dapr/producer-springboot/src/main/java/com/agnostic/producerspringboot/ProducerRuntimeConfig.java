package com.agnostic.producerspringboot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ProducerConfig.class)
class ProducerRuntimeConfig {
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
