package bono.poc

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.kotlin.anyOrNull
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Pact provider verification test class for the Ktor application.
 * This class sets up an embedded Ktor server, configures it, and then
 * uses Pact-JVM to verify interactions defined in consumer pact files.
 *
 * It uses a dynamic port for the embedded server and waits for the server
 * to fully start by monitoring its logs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Provider("superapp-api") // Defines the name of the provider for Pact verification
@PactFolder("pacts") // Specifies the directory where pact files are located OR use Pact Broker to discover and load pacts.
@ExtendWith(PactVerificationInvocationContextProvider::class) // Enables Pact verification context injection
class PactVerificationTest {

    /**
     * Companion object to manage shared resources and lifecycle for all tests in this class.
     * This includes the embedded Ktor server and mock services.
     */
    companion object {
        // Mock service for dependency injection into the Ktor application
        private val pulseService: PulseService = mock()
        // Host and port for the embedded Ktor server. Port is initially 0 for dynamic assignment.
        private var host: String = "0.0.0.0"
        private var port: Int = 0
        // The embedded Ktor server instance
        private lateinit var testApp: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

        // Logback appender to capture logs for server startup verification
        private val logAppender = ListAppender<ILoggingEvent>()
        // Root logger to attach the log appender
        private val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

        /**
         * Sets up the embedded Ktor server before all tests run.
         * This method starts the server, waits for it to become ready by monitoring logs,
         * and retrieves the dynamically assigned port.
         */
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            // Start capturing logs to verify server startup
            logAppender.start()
            rootLogger.addAppender(logAppender)

            // Create and start the embedded Ktor server
            testApp = embeddedServer(Netty, configure = {
                // Configure the connector to bind to the specified host and a dynamic port (if port is 0)
                connectors.add(EngineConnectorBuilder().apply {
                    host = host
                    port = port
                })
            }) {
                // Configure serialization and routing for the Ktor application
                configureSerialization()
                configureRouting(pulseService)
            }.start(wait = false) // Start the server non-blocking

            // Wait for the server to log its startup message, indicating it's ready
            val startTime = System.currentTimeMillis()
            val timeout = 10000L // 10 seconds timeout for server startup
            var serverStarted = false

            while (!serverStarted && System.currentTimeMillis() - startTime < timeout) {
                // Check for common Ktor startup messages in the captured logs
                val startupMessage = logAppender.list.find {
                    it.message.contains("Responding at") || it.message.contains("Application started")
                }
                if (startupMessage != null) {
                    serverStarted = true
                } else {
                    Thread.sleep(100) // Wait a bit before checking logs again
                }
            }

            // If the server didn't start within the timeout, throw an exception
            if (!serverStarted) {
                throw IllegalStateException("Ktor server did not log its startup message within the timeout period.")
            }

            // Get the actual host and port the server bound to after it has started
            // Use resolvedConnectors for the actual bound port, especially when port=0
            val engineConnectorFactory = testApp.engineConfig.connectors.first()
            host = engineConnectorFactory.host
            port = engineConnectorFactory.port
            rootLogger.info("Ktor server started on: $host:$port")
        }

        /**
         * Stops the embedded Ktor server and cleans up log appenders after all tests have run.
         */
        @JvmStatic
        @AfterAll
        fun afterAll() {
            // Stop the Ktor server gracefully
            testApp.stop(1000, 1000)
            // Detach and stop the log appender to prevent memory leaks and interference
            rootLogger.detachAppender(logAppender)
            logAppender.stop()
            logAppender.list.clear() // Clear captured logs for next test run if any
        }
    }

    /**
     * Resets the mock service before each test interaction.
     * This ensures a clean state for each Pact verification.
     */
    @BeforeEach
    fun beforeEach(context: PactVerificationContext) {
        reset(pulseService)
        // Set the target for the Pact verification context to the embedded server
        context.target = HttpTestTarget(host, port)
        rootLogger.info("Verifying request '${context.interaction.description}' with states ${context.interaction.providerStates} against: $host:$port")
    }

    /**
     * Helper function to create a mock Pulse object with a specific value.
     * This is used by the `@State` methods to configure the mock service.
     *
     * @param value The integer value for the Pulse object.
     * @return A new Pulse object with a random ID and the specified value.
     */
    private fun createMockPulse(value: Int): Pulse {
        return Pulse(
            id = UUID.randomUUID().toString(),
            value = value,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = 0L
        )
    }

    /**
     * The main test template for Pact verification.
     * This method is invoked by Pact-JVM for each interaction defined in the pact files.
     *
     * @param context The Pact verification context, providing access to interaction details.
     * @param request The HTTP request object representing the current interaction.
     */
    @TestTemplate
    internal fun pactVerificationTest(context: PactVerificationContext, request: HttpRequest) {
        // Log the request details as defined in the Pact interaction
        rootLogger.info("--- Pact Interaction Request Details ---")
        rootLogger.info("  Method: ${request.method}")
        rootLogger.info("  Path: ${request.path}")
        rootLogger.info("  Full Uri: ${request.uri}") // Full URI including query parameters
        if (request.headers != null && request.headers.isNotEmpty()) {
            rootLogger.info("  Headers: ${request.headers.map { "${it.name}: ${it.value}" }}")
        }
        rootLogger.info("--------------------------------------")

        // Perform the actual Pact verification for the current interaction
        context.verifyInteraction()
    }

    /**
     * Provider state: "Pulse is 1".
     * Configures the mock PulseService to return a Pulse object with value 1.
     */
    @State("Pulse is 1")
    fun pulseIs1() {
        `when`(pulseService.getPulse(anyOrNull())).thenReturn(createMockPulse(1))
    }

    /**
     * Provider state: "Pulse is 2".
     * Configures the mock PulseService to return a Pulse object with value 2.
     */
    @State("Pulse is 2")
    fun pulseIs2() {
        `when`(pulseService.getPulse(anyOrNull())).thenReturn(createMockPulse(2))
    }
}
