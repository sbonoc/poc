package bono.poc

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.mockito.Mockito.*
import org.mockito.kotlin.anyOrNull
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class PulseRoutesTest {

    private val pulseService: PulseService = mock()

    /**
     * Helper function to set up the test application and create a Ktor client.
     * This reduces duplication across test cases.
     */
    private fun withTestClient(block: suspend ApplicationTestBuilder.(client: io.ktor.client.HttpClient) -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureRouting(pulseService)
            }
            val client = createClient {
                install(ClientContentNegotiation) {
                    json()
                }
            }
            block(client)
        }

    /**
     * Helper function to create a mock Pulse object with a specific value.
     */
    private fun createMockPulse(value: Int): Pulse {
        return Pulse(
            id = UUID.randomUUID().toString(),
            value = value,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = 0 // Assuming 'version' is the correct field, not 'deletedAt' based on Pulse.kt
        )
    }

    @Test
    fun `GET api pulses should return pulse with value 1`() = withTestClient { client ->
        `when`(pulseService.getPulse(anyOrNull())).thenReturn(createMockPulse(1))

        client.get("/api/pulses").apply {
            assertEquals(HttpStatusCode.OK, status)
            val pulse = body<Pulse>()
            assertNotNull(pulse)
            assertEquals(1, pulse.value)
        }
    }

    @Test
    fun `GET api pulses should return pulse with value 2`() = withTestClient { client ->
        `when`(pulseService.getPulse(anyOrNull())).thenReturn(createMockPulse(2))

        client.get("/api/pulses").apply {
            assertEquals(HttpStatusCode.OK, status)
            val pulse = body<Pulse>()
            assertNotNull(pulse)
            assertEquals(2, pulse.value)
        }
    }

    @Test
    fun `GET api pulses with invalid from parameter should return BadRequest`() = withTestClient { client ->
        client.get("/api/pulses?from=invalid-date").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            val errorResponse = body<Map<String, String>>()
            assertEquals(
                "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD).",
                errorResponse["error"]
            )
        }
    }

    @Test
    fun `GET api pulses with valid date from parameter should return OK`() = withTestClient { client ->
        `when`(pulseService.getPulse(anyOrNull())).thenReturn(createMockPulse(10))

        client.get("/api/pulses?from=2023-01-15").apply {
            assertEquals(HttpStatusCode.OK, status)
            val pulse = body<Pulse>()
            assertNotNull(pulse)
            assertEquals(10, pulse.value)
        }
    }

}
