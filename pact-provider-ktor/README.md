# Pact Provider for Ktor

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Project Overview

This project demonstrates a Ktor application acting as a Pact provider. It showcases how to set up a Ktor server, define API routes, and integrate Pact-JVM for provider-side contract testing. The application provides a simple "Pulse" API, and the tests verify that the API adheres to contracts defined by a consumer.

## Features

Here's a list of features included in this project:

| Name                                                                   | Description                                                                        |
| ------------------------------------------------------------------------|------------------------------------------------------------------------------------ |
| [Routing](https://start.ktor.io/p/routing)                             | Provides a structured routing DSL                                                  |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |

## Step-by-Step Setup and Development

This section outlines the key steps taken to set up and develop this Ktor Pact Provider project.

### Step 1: Create the Ktor project ###
*   The project was initialized using the Ktor Project Generator with the following configuration:
        [https://start.ktor.io/settings?name=poc&website=bono&artifact=bono.poc&kotlinVersion=2.1.10&ktorVersion=3.2.3&buildSystem=GRADLE_KTS&buildSystemArgs.version_catalog=true&engine=NETTY&configurationIn=YAML&addSampleCode=true&plugins=routing%252Ccontent-negotiation%252Crequest-validation](https://start.ktor.io/settings?name=poc&website=bono&artifact=bono.poc&kotlinVersion=2.1.10&ktorVersion=3.2.3&buildSystem=GRADLE_KTS&buildSystemArgs.version_catalog=true&engine=NETTY&configurationIn=YAML&addSampleCode=true&plugins=routing%252Ccontent-negotiation%252Crequest-validation)
*   This provided a foundational Ktor application with essential features like routing, content negotiation, and request validation.

### Step 2: Add Pact, JUnit5, and Mockito dependencies ###
*   To enable contract testing with Pact and unit testing with Mockito, the `gradle/libs.versions.toml` and `build.gradle.kts` files were updated to include the necessary dependencies.
*   Key additions in `gradle/libs.versions.toml`:
    ```toml
    [versions]
    # ... existing versions
    mockito = "5.19.0"
    mockito-kotlin = "5.3.1"
    pact = "4.6.17"
    junit = "5.13.4"

    [libraries]
    # ... existing libraries
    mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
    mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version.ref = "mockito-kotlin" }
    junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
    kotlin-test-junit5 = { module = "org.jetbrains.kotlin:kotlin-test-junit5", version.ref = "kotlin" }
    pact-provider = { module = "au.com.dius.pact.provider:junit5", version.ref="pact" }
    ```
  *   Corresponding additions in `build.gradle.kts`:
      ```kotlin
      dependencies {
          // ... existing dependencies
          testImplementation(libs.mockito.core)
          testImplementation(libs.mockito.kotlin)
          testImplementation(libs.pact.provider)
          testImplementation(libs.junit.jupiter)
          testImplementation(libs.kotlin.test.junit5)
      }
        
      tasks.test {
          useJUnitPlatform()
      }
      ```

### Step 3: Create the actual Ktor application code ###

The core application logic, including data models (`Pulse.kt`), service interfaces (`PulseService.kt`), and the implementation of API routes (`PulseRoutes.kt`), was developed in the `src/main/kotlin` directory. This defines the `/api/pulses` endpoint and its behavior.

### Step 4: Add Ktor Tests to check application works as expected ###
*   Standard Ktor integration tests were implemented in `src/test/kotlin/PulseRoutesTest.kt`. These tests use Ktor's `testApplication` and `createClient` to verify the functionality of the `/api/pulses` endpoint, ensuring it responds correctly to various requests and parameters.
*   The idea is that the provider must keep doing their internal tests for their API as they wish and probably more detailed.

### Step 5: Add the Pact Provider verification tests for contract testing ###
* **Understanding Pact and Contract Testing:** While traditional Ktor integration tests (like those in Step 4) are crucial for verifying the *internal logic and functionality* of your API, they don't guarantee compatibility with external consumers. This is where **Pact (Consumer-Driven Contract Testing)** comes in. Pact ensures that your API (the "provider") adheres to the expectations (the "contracts") defined by its consumers. This prevents breaking changes and allows consumer and provider teams to develop and deploy independently with confidence.

* The provider-side contract tests were set up in `src/test/kotlin/PactVerificationTest.kt`. This class leverages Pact-JVM's JUnit 5 integration to:
    * **Isolate and Verify:** It starts an embedded Ktor server specifically for these tests, ensuring a controlled environment.
        * Corresponding code in `PactVerificationTest.kt`:
            ```kotlin
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
            ```
  *   **Define Provider States:** Using `@State` annotations (e.g., "Pulse is 1", "Pulse is 2"), the tests define specific scenarios or data conditions that the provider needs to be in for a given interaction. This allows the mock `PulseService` to be configured to return predictable data, mimicking real-world states.
      * Corresponding code in `PactVerificationTest.kt`:
      ```kotlin
      @State("Pulse is 1")
      fun pulseIs1() {
        `when`(pulseService.getPulse(anyOrNull())).thenReturn(createMockPulse(1))
      }
      ```
  *   **Automate Verification:** It uses `@TestTemplate` and `PactVerificationInvocationContextProvider` to automatically iterate through and verify each interaction defined in the consumer pact files. These pact files, generated by the consumer's tests, describe the expected requests and responses. In this project, they are located in `../pact-consumer-vue/pacts/`.
      * Corresponding code in `PactVerificationTest.kt`: 
      ```kotlin
      @TestTemplate
      internal fun pactVerificationTest(context: PactVerificationContext, request: HttpRequest) {
        // Perform the actual Pact verification for the current interaction
        context.verifyInteraction()
      }
      ```
  *   **Key Advantage:** These Pact tests are a vital addition to your standard Ktor integration tests (from Step 4). They specifically verify the contracts that consumers rely on, guaranteeing that any changes to your API do not inadvertently break existing consumer integrations. This provides a strong safety net, especially in microservices architectures or when multiple teams depend on your API.

## Building & Running

To build or run the project, use one of the following tasks:

| Task                   | Description                                                |
|------------------------|------------------------------------------------------------|
| `./gradlew test`       | Run the tests (including Pact provider verification tests) |
| `./gradlew build`      | Build everything (via Gradle)                              |
| `./gradlew run`        | Run the server (via Gradle)                                |
| `docker compose build` | Build everything (via Docker)                              |
| `docker compose up`    | Run the server (via Docker)                                |
