package com.agnostic.producerspringboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class ProducerServiceTest {
    @Test
    void publishReturnsAcceptedWhenDaprRespondsSuccess() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), org.mockito.ArgumentMatchers.eq(Void.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        ProducerConfig config = new ProducerConfig("order-pubsub", "orders", 3500);
        ProducerService service = new ProducerService(restTemplate, config, new SimpleMeterRegistry());

        ResponseEntity<Map<String, String>> response =
            service.publish(new PublishOrderRequest("ORD-1", new BigDecimal("10.50")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "accepted").containsEntry("orderId", "ORD-1");
    }
}
