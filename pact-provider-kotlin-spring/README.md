# Read Me First
The following was discovered as part of building this project:

* No Docker Compose services found. As of now, the application won't start! Please add at least one service to the `compose.yaml` file.
* The original package name 'bono.poc.pact-provider-kotlin-spring' is invalid and this project uses 'bono.poc.pact_provider_kotlin_spring' instead.

# Getting Started

### Project Overview: `pact-provider-kotlin-spring`
This project serves as a **Pact Provider** application built with **Spring Boot and Kotlin**. It exposes a reactive REST API for "Pulse" data and demonstrates how to implement **Pact contract testing** for provider verification. The primary goal is to ensure that the API provided by this service adheres to the expectations defined by its consumers, preventing integration issues and fostering independent deployments.

### Step-by-step Guide

This section outlines the steps taken to set up and configure this Spring Boot application as a Pact provider.

#### Step 1: Project Initialization with Spring Initializr
The project was initially generated using [Spring Initializr](https://start.spring.io/) with the following key dependencies:
*   **Lombok**: For reducing boilerplate code (e.g., getters, setters, constructors).
*   **Spring Reactive Web (WebFlux)**: To build a reactive, non-blocking web application.

#### Step 2: Implementing the Reactive REST API for Pulse
A simple reactive REST API for "Pulse" data was created. This API typically involves:
*   **`Pulse` Model**: A data class representing the `Pulse` entity (e.g., `id`, `value`, `createdAt`).
*   **`PulseService`**: A service layer responsible for business logic related to `Pulse` data. In this example, it provides a method to retrieve `Pulse` information.
*   **`PulseHandler`**: A handler component that implements the REST endpoint(s) for `Pulse` data, interacting with the `PulseService`.
*   **`PulseRouter`**: A configuration bean that exposes the REST endpoint(s) for `Pulse`, using the `PulseHandler`.

You can find the core implementation files under `src/main/kotlin/bono/poc/pact/provider/`.

#### Step 3: Adding Dependencies for Pact and Testing
To enable Pact provider verification and robust testing, the following dependencies were added to `build.gradle.kts`:
*   **`org.mockito.kotlin`**: For mocking dependencies in unit and integration tests, as well as for managing the Pact States easily without the need of complex setups.
*   **`au.com.dius.pact.provider:junit5`**: The core Pact-JVM library for JUnit 5 provider testing.
*   **`au.com.dius.pact.provider:spring`**: Provides Spring-specific integrations for Pact provider testing, allowing the Spring application context to be managed during tests.

These dependencies allow the project to run provider verification tests against consumer-generated pact files.

#### Step 4: Understanding the Test Landscape: `PactVerificationTest` vs. `PulseHandlerTest`

In this project, two main types of tests are relevant:

*   **`PactVerificationTest`**: This is a **contract test** (as integration). Its primary purpose is to verify that the provider's API adheres to the expectations defined by its consumers. It does this by replaying requests from consumer-generated pact files against a running instance of the provider application mocking the service layer to avoid complex setups and be able to run as integration tests. This test ensures compatibility between services.
*   **`PulseHandlerTest`**: This is an **integration test** focused specifically on the `PulseHandler` component. This test is 100% done by the provider's developers without intervention of the consumers. Normally it tests all logic the provider team may consider.

#### Step 5: Deep Dive into `PactVerificationTest`

The `PactVerificationTest` class (`src/test/kotlin/bono/poc/pact/provider/PactVerificationTest.kt`) is the cornerstone of provider-side contract testing.

*   **`@Provider("superapp-api")`**: This annotation identifies the name of this provider service, which must match the provider name specified in the consumer's pact file.
*   **`@PactFolder("../pact-consumer-vue/pacts")`**: This annotation tells Pact-JVM where to find the consumer pact JSON files. In this setup, it points to the `pacts` directory within the `pact-consumer-vue` project, indicating that the consumer is a Vue.js application.
*   **`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`**: This annotation starts the full Spring Boot application context for the test, listening on a random available port. This ensures that the actual API endpoints are tested.
*   **`@ExtendWith(PactVerificationInvocationContextProvider::class)`**: This JUnit 5 extension integrates Pact-JVM's test execution model, allowing it to dynamically generate tests for each interaction defined in the pact files.
*   **`@MockitoBean private var pulseService: PulseService = mock()`**: This is a crucial part of setting up provider states. Instead of relying on a real `PulseService` (which might interact with a database), we mock it. This allows us to control the service's behavior precisely for each provider state.
*   **`@LocalServerPort private var port: Int = 0`**: Injects the dynamically assigned port of the running Spring Boot application.
*   **`@BeforeEach fun setUp(context: PactVerificationContext)`**: Before each test interaction, this method sets the `context.target` to `HttpTestTarget("localhost", port)`. This tells Pact-JVM where to send the HTTP requests for verification.
*   **`@TestTemplate fun verifyPact(context: PactVerificationContext)`**: This is the main test method. Pact-JVM will invoke this method for each interaction found in the pact files. `context.verifyInteraction()` performs the actual HTTP request against the running provider and verifies the response against the contract. The `verify(pulseService, atLeastOnce()).getPulse(any<LocalDate>())` line ensures that our mocked service was indeed called during the interaction, confirming the flow.
*   **`@State("Pulse is 1") fun setPulseToOne()` and `@State("Pulse is 2") fun setPulseToTwo()`**: These are **provider state methods**. When a consumer interaction specifies a particular "provider state" (e.g., "Pulse is 1"), Pact-JVM calls the corresponding `@State` method *before* sending the request. These methods are responsible for setting up the provider's backend (in this case, configuring the mocked `pulseService`) to return the data expected by the consumer for that specific state. This ensures that the provider is in the correct data state for the interaction being verified. The use of `Mono.just` reflects the reactive nature of the `PulseService`.

#### Step 6: `PulseHandlerTest`
The `PulseHandlerTest` (`src/test/kotlin/bono/poc/pact/provider/handler/PulseHandlerTest.kt`) focuses on testing the internal logic and behavior of the `PulseHandler` component in isolation. Unlike `PactVerificationTest` which validates external contracts with consumers, `PulseHandlerTest` ensures the handler's correctness, request mapping, and response generation by simulating HTTP requests using `WebTestClient` and mocking its direct dependencies (like `PulseService`). This test is purely provider-driven and verifies the handler's adherence to internal business rules and error handling, without involving consumer expectations.

# Why Pact? Benefits Over Traditional Integration Tests

For those skeptical about adopting Pact, here's why it offers significant advantages over traditional integration tests:

*   **Traditional Integration Tests (Provider-Side)**:
    *   Often involve testing the provider's API in isolation or with a dummy consumer.
    *   **Problem**: They don't guarantee compatibility with *actual* consumers. You might pass all your internal integration tests, only to find out in a staging environment that a consumer's real-world usage breaks your API. This leads to "integration hell" where issues are found late in the development cycle.
    *   **Problem**: They require complex setup to simulate various consumer requests and data states, often leading to brittle and hard-to-maintain test suites.

*   **Pact Contract Tests (Consumer-Driven)**:
    *   **Consumer-Driven Guarantee**: Consumers define their expectations (the "contract"), and the provider verifies that it meets these *actual* expectations. This ensures the provider's API is truly compatible with its known consumers.
    *   **Early Feedback**: Breaks in the contract are detected much earlier in the development pipeline, often during CI/CD, before deployment to higher environments. This significantly reduces the cost and effort of fixing issues.
    *   **Reduced Integration Risk**: By continuously verifying against consumer contracts, Pact provides a high degree of confidence that the provider's API will work seamlessly with its consumers, minimizing integration surprises.
    *   **Clear Communication & Living Documentation**: The pact file itself serves as a clear, executable, and living documentation of the API contract between the consumer and provider teams.
    *   **Isolation & Efficiency**: Provider tests can run in isolation, mocking out external dependencies (like databases or other services), while still verifying against real consumer expectations. This makes tests faster, more reliable, and less dependent on complex environments.
    *   **Trust Between Teams**: Pact fosters collaboration and builds trust between teams developing interdependent services by providing a shared understanding and automated verification of their API contracts.

In essence, Pact shifts the focus from "does my API work?" to "does my API work *for my consumers*?", providing a more robust and efficient way to manage microservice integrations.

### Reference Documentation
For further reference, please consider the following sections:

*   [Official Gradle documentation](https://docs.gradle.org)
*   [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.5/gradle-plugin)
*   [Create an OCI image](https://docs.spring.io/spring-boot/3.5.5/gradle-plugin/packaging-oci-image.html)
*   [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/6.2.10/languages/kotlin/coroutines.html)
*   [Docker Compose Support](https://docs.spring.io/spring-boot/3.5.5/reference/features/dev-services.html#features.dev-services.docker-compose)
*   [Spring Reactive Web](https://docs.spring.io/spring-boot/3.5.5/reference/web/reactive.html)

### Guides
The following guides illustrate how to use some features concretely:

*   [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)

### Additional Links
These additional references should also help you:

*   [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.

However, no services were found. As of now, the application won't start!

Please make sure to add at least one service in the `compose.yaml` file.
