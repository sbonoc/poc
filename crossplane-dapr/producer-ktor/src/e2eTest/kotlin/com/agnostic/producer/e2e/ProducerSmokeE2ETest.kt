package com.agnostic.producer.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ProducerSmokeE2ETest {
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E", matches = "true")
    fun `ready endpoint responds in deployed environment`() {
        val baseUrl = System.getenv("E2E_PRODUCER_BASE_URL") ?: "http://localhost:8080"
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/health/ready")).GET().build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        assertThat(response.statusCode()).isEqualTo(200)
    }
}
