package com.agnostic.consumerspringboot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsumerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void subscribeEndpointReturnsConfiguredContract() {
        ResponseEntity<String> response = restTemplate.getForEntity("/dapr/subscribe", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("pubsubname").contains("topic").contains("route");
    }
}
