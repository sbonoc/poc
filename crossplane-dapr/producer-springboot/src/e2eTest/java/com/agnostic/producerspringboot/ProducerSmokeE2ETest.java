package com.agnostic.producerspringboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class ProducerSmokeE2ETest {
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E", matches = "true")
    void readyEndpointRespondsInDeployedEnvironment() throws Exception {
        String baseUrl = System.getenv().getOrDefault("E2E_PRODUCER_SPRINGBOOT_BASE_URL", "http://localhost:8084");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/health/readiness")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
