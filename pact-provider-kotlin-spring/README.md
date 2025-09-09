# Pact Provider for Kotlin and Spring

This project serves as a **Pact Provider** application built with **Spring Boot and Kotlin**. It exposes a reactive REST API for "Pulse" data and demonstrates how to implement **Pact contract testing** for provider verification. The primary goal is to ensure that the API provided by this service adheres to the expectations defined by its consumers, preventing integration issues and fostering independent deployments.

Table of contents:
*   [Why Pact? Beyond Traditional Integration Tests](#why-pact-beyond-traditional-integration-tests)
*   [Features](#features)
*   [Building & Running](#building--running)
*   [Step-by-Step Setup and Development](#step-by-step-setup-and-development)
    *   [Step 1: Project Initialization](#step-1-project-initialization)
    *   [Step 2: Implementing the Reactive REST API for Pulse](#step-2-implementing-the-reactive-rest-api-for-pulse)
    *   [Step 3: Adding Dependencies for Pact and Testing](#step-3-adding-dependencies-for-pact-and-testing)
    *   [Step 4: Understanding the Test Landscape: `PactVerificationTest` vs. `PulseHandlerTest`](#step-4-understanding-the-test-landscape-pactverificationtest-vs-pulsehandlertest)
    *   [Step 5: Deep Dive into `PactVerificationTest`](#step-5-deep-dive-into-pactverificationtest)
    *   [Step 6: `PulseHandlerTest`](#step-6-pulsehandlertest)
*   [Reference Documentation](#reference-documentation)
*   [Useful Links](#useful-links)
*   [Guides](#guides)

## Why Pact? Beyond Traditional Integration Tests

Still relying on traditional integration tests? Here's why Pact offers a superior approach for microservices:

**Traditional Integration Tests (Provider-Side):**
*   **Blind Spots**: Pass internal tests, but still break *actual* consumers in production. Integration issues found late, leading to costly fixes.
*   **Brittle & Slow**: Complex setup, tightly coupled, and often slow, making them hard to maintain and run frequently.

**Pact Contract Tests (Consumer-Driven):**
*   **Guaranteed Compatibility**: Consumers define their exact needs. Providers verify they meet these *real-world* expectations, ensuring true compatibility.
*   **Shift Left**: Catch integration bugs in CI/CD, not production. Faster feedback loops mean quicker fixes and less debugging.
*   **Independent Deployments**: Confidently deploy services knowing they won't break consumers, enabling true microservice autonomy.
*   **Clear Contracts**: Pact files are living, executable documentation of API agreements, fostering better team communication.
*   **Fast & Reliable**: Tests run in isolation with mocked dependencies, making them quick, stable, and easy to maintain.

**In short: Pact ensures your API works *for your consumers*, not just in isolation. It's about confidence, speed, and seamless microservice integration.**

## Features

Here's a list of key features and technologies used in this project:

| Name                                                                                     | Description                                                                  |
|------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| [**Spring Boot**](https://spring.io/projects/spring-boot)                                | Rapid application development with auto-configuration and embedded servers.  |
| [**Spring WebFlux**](https://docs.spring.io/spring-framework/reference/web/webflux.html) | Reactive web framework for building non-blocking, asynchronous applications. |
| [**Kotlin**](https://kotlinlang.org/)                                                    | Modern, concise, and safe programming language for JVM.                      |
| [**Pact-JVM**](https://docs.pact.io/implementation_guides/jvm)                           | Framework for Consumer-Driven Contract testing.                              |
| [**Mockito**](https://site.mockito.org/)                                                 | Mocking framework for unit and integration tests.                            |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                   | Description                                                |
|------------------------|------------------------------------------------------------|
| `./gradlew test`       | Run the tests (including Pact provider verification tests) |
| `./gradlew build`      | Build everything (via Gradle)                              |
| `./gradlew bootRun`    | Run the Spring Boot application (via Gradle)               |
| `docker compose build` | Build everything (via Docker)                              |
| `docker compose up`    | Run the server (via Docker)                                |

## Step-by-Step Setup and Development

This section outlines the steps taken to set up and configure this Spring Boot application as a Pact provider.

#### Step 1: Project Initialization
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
*   **`@TestTemplate fun verifyPact(context: PactVerificationContext)`**: This is the main test method. Pact-JVM will invoke this method for each interaction defined in the pact files. `context.verifyInteraction()` performs the actual HTTP request against the running provider and verifies the response against the contract. The `verify(pulseService, atLeastOnce()).getPulse(any<LocalDate>())` line ensures that our mocked service was indeed called during the interaction, confirming the flow.
*   **`@State("Pulse is 1") fun setPulseToOne()` and `@State("Pulse is 2") fun setPulseToTwo()`**: These are **provider state methods**. When a consumer interaction specifies a particular "provider state" (e.g., "Pulse is 1"), Pact-JVM calls the corresponding `@State` method *before* sending the request. These methods are responsible for setting up the provider's backend (in this case, configuring the mocked `pulseService`) to return the data expected by the consumer for that specific state. This ensures that the provider is in the correct data state for the interaction being verified. The use of `Mono.just` reflects the reactive nature of the `PulseService`.

#### Step 6: `PulseHandlerTest`
The `PulseHandlerTest` (`src/test/kotlin/bono/poc/pact/provider/handler/PulseHandlerTest.kt`) focuses on testing the internal logic and behavior of the `PulseHandler` component in isolation. Unlike `PactVerificationTest` which validates external contracts with consumers, `PulseHandlerTest` ensures the handler's correctness, request mapping, and response generation by simulating HTTP requests using `WebTestClient` and mocking its direct dependencies (like `PulseService`). This test is purely provider-driven and verifies the handler's adherence to internal business rules and error handling, without involving consumer expectations.

### Reference Documentation
For further reference, please consider the following sections:

*   [Official Gradle documentation](https://docs.gradle.org)
*   [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.5/gradle-plugin)
*   [Create an OCI image](https://docs.spring.io/spring-boot/3.5.5/gradle-plugin/packaging-oci-image.html)
*   [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/6.2.10/languages/kotlin/coroutines.html)
*   [Docker Compose Support](https://docs.spring.io/spring-boot/3.5.5/reference/features/dev-services.html#features.dev-services.docker-compose)
*   [Spring Reactive Web](https://docs.spring.io/spring-boot/3.5.5/reference/web/reactive.html)

### Useful Links

*   **Kotlin Official Documentation**: [https://kotlinlang.org/docs/home.html](https://kotlinlang.org/docs/home.html)
*   **Spring Boot Official Documentation**: [https://docs.spring.io/spring-boot/docs/current/reference/html/](https://docs.spring.io/spring-boot/docs/current/reference/html/)
*   **Spring Initializr**: [https://start.spring.io/](https://start.spring.io/)
*   **Pact-JVM Provider Testing**: [https://docs.pact.io/implementation_guides/jvm/provider](https://docs.pact.io/implementation_guides/jvm/provider)

### Guides
The following guides illustrate how to use some features concretely:

*   [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)
