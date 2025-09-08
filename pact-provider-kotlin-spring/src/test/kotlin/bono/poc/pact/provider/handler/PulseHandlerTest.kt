package bono.poc.pact.provider.handler

import bono.poc.pact.provider.service.PulseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDate
import java.util.*

class PulseHandlerTest {

    private lateinit var pulseService: PulseService
    private lateinit var pulseHandler: PulseHandler

    @BeforeEach
    fun setUp() {
        pulseService = mock(PulseService::class.java)
        pulseHandler = PulseHandler(pulseService)
    }

    @Test
    fun `getPulses should return BadRequest for invalid date parameter`() {
        val mockRequest = mock(ServerRequest::class.java)

        `when`(mockRequest.queryParam("from")).thenReturn(Optional.of("invalid-date"))

        val responseMono = pulseHandler.getPulses(mockRequest)

        StepVerifier.create(responseMono)
            .expectNextMatches { response ->
                response.statusCode() == HttpStatus.BAD_REQUEST &&
                response.headers().contentType == MediaType.APPLICATION_JSON
            }
            .verifyComplete()

        verifyNoInteractions(pulseService) // PulseService should not be called for invalid input
    }

    @Test
    fun `getPulses should handle PulseService error`() {
        val today = LocalDate.now()
        val mockRequest = mock(ServerRequest::class.java)
        val errorMessage = "Service unavailable"

        `when`(mockRequest.queryParam("from")).thenReturn(Optional.of("today"))
        `when`(pulseService.getPulse(today)).thenReturn(Mono.error(RuntimeException(errorMessage)))

        val responseMono = pulseHandler.getPulses(mockRequest)

        StepVerifier.create(responseMono)
            .expectNextMatches { response ->
                response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR && // Or whatever error status you want to map
                response.headers().contentType == MediaType.APPLICATION_JSON
            }
            .verifyComplete()

        verify(pulseService).getPulse(today)
    }
}
