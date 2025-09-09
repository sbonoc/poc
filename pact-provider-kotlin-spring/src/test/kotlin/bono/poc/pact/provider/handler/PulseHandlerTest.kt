package bono.poc.pact.provider.handler

import bono.poc.pact.provider.model.Pulse
import bono.poc.pact.provider.service.PulseService
import bono.poc.pact.provider.router.PulseRouter // Import the existing PulseRouter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import java.util.*

// Explicitly include PulseRouter in the classes array for WebFluxTest
@WebFluxTest(controllers = [PulseHandler::class, PulseRouter::class])
class PulseHandlerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient // Injects WebTestClient for making requests

    @MockitoBean // Spring provides a mock for PulseService and injects it into PulseHandler
    private lateinit var pulseService: PulseService

    @Test
    fun `getPulses should return BadRequest for invalid date parameter`() {
        webTestClient.get().uri("/api/pulses?from=invalid-date")
            .exchange() // Perform the request
            .expectStatus().isBadRequest // Assert HTTP status
            .expectHeader().contentType(MediaType.APPLICATION_JSON) // Assert Content-Type header
            .expectBody() // Get the response body
            .jsonPath("$.error").isEqualTo("Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD).") // Assert specific JSON content

        // Verify that the service was not called for invalid input
        verifyNoInteractions(pulseService)
    }

    @Test
    fun `getPulses should handle PulseService error`() {
        val today = LocalDate.now()
        val errorMessage = "Service unavailable"

        // Configure the mock service to return an error Mono
        `when`(pulseService.getPulse(today)).thenReturn(Mono.error(RuntimeException(errorMessage)))

        webTestClient.get()
            .uri("/api/pulses?from=today")
            .headers { headers -> headers.set(ACCEPT, APPLICATION_JSON_VALUE) }
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR) // Assert HTTP status
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.error").isEqualTo("Internal Server Error: $errorMessage") // Assert specific JSON content

        // Verify that the service method was called
        verify(pulseService).getPulse(today)
    }

    @Test
    fun `getPulses should return pulse for valid today parameter`() {
        val today = LocalDate.now()
        val expectedPulse = Pulse(
            id = UUID.randomUUID().toString(),
            value = 10,
            createdAt = today.toEpochDay(),
            updatedAt = Instant.now().toEpochMilli(),
            deletedAt = 0
        )

        // Configure the mock service to return a successful Mono with the expected Pulse
        `when`(pulseService.getPulse(today)).thenReturn(Mono.just(expectedPulse))

        webTestClient.get().uri("/api/pulses?from=today")
            .exchange()
            .expectStatus().isOk // Assert HTTP status
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(Pulse::class.java) // Assert and deserialize the body to Pulse class
            .isEqualTo(expectedPulse) // Assert the deserialized object matches the expected one

        // Verify that the service method was called
        verify(pulseService).getPulse(today)
    }

    @Test
    fun `getPulses should return pulse for valid date parameter`() {
        val specificDate = LocalDate.of(2023, 1, 15)
        val expectedPulse = Pulse(
            id = UUID.randomUUID().toString(),
            value = 20,
            createdAt = specificDate.toEpochDay(),
            updatedAt = Instant.now().toEpochMilli(),
            deletedAt = 0
        )

        // Configure the mock service to return a successful Mono with the expected Pulse
        `when`(pulseService.getPulse(specificDate)).thenReturn(Mono.just(expectedPulse))

        webTestClient.get().uri("/api/pulses?from=2023-01-15")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(Pulse::class.java)
            .isEqualTo(expectedPulse)

        // Verify that the service method was called
        verify(pulseService).getPulse(specificDate)
    }
}
