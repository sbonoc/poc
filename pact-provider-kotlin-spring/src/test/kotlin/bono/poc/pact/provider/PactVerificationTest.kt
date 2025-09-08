package bono.poc.pact.provider

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import bono.poc.pact.provider.model.Pulse
import bono.poc.pact.provider.service.PulseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Pact Provider Verification Test.
 * This class verifies that the Spring Boot application (provider)
 * adheres to the contracts defined by its consumers.
 *
 * It uses JUnit 5 and Pact-JVM to run the verification.
 * The `@PactFolder` annotation points to the directory where consumer pact files are located.
 * In a real-world scenario, you might use `@PactBroker` to fetch pacts from a Pact Broker.
 */
@Provider("superapp-api") // The name of your provider as defined in the consumer pact
@PactFolder("../pact-consumer-vue/pacts") // Directory where consumer pact JSON files are stored (e.g., src/test/resources/pacts)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PactVerificationInvocationContextProvider::class)
class PactVerificationTest {

    @MockitoBean
    private var pulseService: PulseService = mock() // Mock the PulseService for testing purposes

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp(context: PactVerificationContext) {
        // Set up the target for the Pact verifier to point to your running Spring Boot app
        context.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    fun verifyPact(context: PactVerificationContext) {
        // This method will be called for each interaction defined in the pact files
        context.verifyInteraction()
        verify(pulseService, atLeastOnce()).getPulse(any<LocalDate>())
    }

    @State("Pulse is 1")
    fun setPulseToOne() {
        println("Provider state: Pulse is 1")
        // Configure the mock PulseService to return a Pulse with value 1
        whenever(pulseService.getPulse(any<LocalDate>())).thenReturn(
            Mono.just(
                Pulse(
                    id = UUID.randomUUID().toString(),
                    value = 1,
                    createdAt = LocalDate.now().toEpochDay(),
                    updatedAt = Instant.now().toEpochMilli(),
                    deletedAt = 0
                )
            )
        )
    }

    @State("Pulse is 2")
    fun setPulseToTwo() {
        println("Provider state: Pulse is 2")
        // Configure the mock PulseService to return a Pulse with value 2
        whenever(pulseService.getPulse(any<LocalDate>())).thenReturn(
            Mono.just(
                Pulse(
                    id = UUID.randomUUID().toString(),
                    value = 2,
                    createdAt = LocalDate.now().toEpochDay(),
                    updatedAt = Instant.now().toEpochMilli(),
                    deletedAt = 0
                )
            )
        )
    }
}
