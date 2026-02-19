package com.agnostic.consumer.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ConsumerSmokeE2ETest {
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E", matches = "true")
    fun `subscribe endpoint responds in deployed environment`() {
        val baseUrl = System.getenv("E2E_CONSUMER_BASE_URL") ?: "http://localhost:8081"
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/dapr/subscribe")).GET().build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        assertThat(response.statusCode()).isEqualTo(200)
    }
}
