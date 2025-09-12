# Pact Provider for Go

This project showcases a Go and Gin-based backend service, illustrating the implementation of Pact for provider-side contract testing.

## Table of Contents
* [Why Pact? Beyond Traditional Integration Tests](#why-pact-beyond-traditional-integration-tests)
* [Features](#features)
* [Building & Running](#building--running)
  * [Pre-requisites](#pre-requisites)
* [Step-by-Step Setup and Development](#step-by-step-setup-and-development)
  * [Step 1: Create the Go project](#step-1-create-the-go-project)
  * [Step 2: Create the actual application using Gin](#step-2-create-the-actual-application-using-gin)
  * [Step 3: Add Go Test to check application works as expected](#step-3-add-go-test-to-check-application-works-as-expected)
  * [Step 4: Add the Pact Provider verification tests for contract testing](#step-4-add-the-pact-provider-verification-tests-for-contract-testing)
* [Reference Documentation](#reference-documentation)
* [Guides](#guides)

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

| Name                                                      | Description                                                                |
|:----------------------------------------------------------|:---------------------------------------------------------------------------|
| [Go](https://go.dev/)                                     | The Go programming language.                                               |
| [Gin Web Framework](https://gin-gonic.com/)               | A high-performance HTTP web framework for Go.                              |
| [Pact-Go](https://docs.pact.io/implementation_guides/go/) | A Go implementation of Pact, a consumer-driven contract testing framework. |
| [Testify](https://github.com/stretchr/testify)            | A Go testing toolkit providing assertions and mocks.                       |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                   | Description                                                                                                |
|:-----------------------|:-----------------------------------------------------------------------------------------------------------|
| `go build`             | Builds the Go application.                                                                                 |
| `go test`              | Runs all unit and integration tests, including Pact provider tests.                                        |
| `go run main.go`       | Starts the Gin web server.                                                                                 |
| `docker compose build` | (Docker) Builds the Go application and runs all unit and integration tests, including Pact provider tests. |
| `docker compose up`    | (Docker) Starts the Gin web server.                                                                        |

### Pre-requisites

Follow the installation instructions of Pact-Go here: https://docs.pact.io/implementation_guides/go/readme#installation 

Find below the steps I followed:
1. Install Go via Homebrew: `brew install go`.
2. Add the following in the bash profile `.zshrc`:
    ```bash
   export GOPATH=$HOME/go
   export PATH=$PATH:$GOPATH/bin
   ```
3. Install library in project: `go get github.com/pact-foundation/pact-go/v2`.
4. Install Pact Go CLI in OS: `go install github.com/pact-foundation/pact-go/v2`.
5. Execute Pact Go CLI installation to setup Pact in the OS: `pact-go install`.
6. Now when executing `go test` the Pact Provider's tests have all necessary libraries to be executed.

## Step-by-Step Setup and Development

### Step 1: Create the Go project

Initialize a Go module:
```bash
go mod init bono.poc/pact-provider-go
```

### Step 2: Create the actual application using Gin

This step involves creating the core Go application using the Gin web framework. We'll define a simple API that provides "pulse" data.

First, add Gin to the project:
```bash
go get github.com/gin-gonic/gin
```
The project follows a common Go project structure, separating concerns into internal packages:
* `main.go`: This is the application's entry point. It's responsible for setting up the Gin router, initializing the necessary services and handlers, and starting the HTTP server. It acts as the composition root for the application.
* `internal/model`: This package defines the data structures (structs) that represent the core entities of our application. For this project, it contains the Pulse struct, which models the data returned by our API.
* `internal/service`: This package encapsulates the business logic. It defines interfaces (e.g., PulseService) and their implementations. Services are responsible for performing operations on the data, independent of how that data is exposed (e.g., via HTTP). They interact with data sources (though in this example, it's mocked data).
* `internal/handler`: This package contains the HTTP handlers (often called controllers in other frameworks). These handlers receive incoming HTTP requests, parse them, interact with the service layer to perform the requested operation, and then format and send back the HTTP response.

This layered architecture is crucial for testability. By defining clear interfaces for services and injecting them into handlers, we can easily substitute real service implementations with mock implementations during testing. This allows us to isolate components and test them independently, making unit and integration tests more reliable and faster.

### Step 3: Add Go Test to check application works as expected

To ensure our application's HTTP handlers function correctly, we implement integration tests for the `PulseHandler`. These tests focus on the handler's logic in isolation, using a mock implementation of the PulseService to control the behavior of its dependencies. This approach allows us to test various scenarios (e.g., successful response, service error) without needing a running database or external service.
The tests are located in `internal/handler/pulse_test.go`.

The idea is that the provider must keep doing their internal tests for their API as they wish and probably more detailed.

### Step 4: Add the Pact Provider verification tests for contract testing

This step details how to set up and run Pact provider verification tests. These tests ensure that our Go API (the provider) adheres to the contracts defined by its consumers.
First, ensure you have the `pact-go/v2` and `stretchr/testify` libraries installed in your project:
```bash
go get github.com/pact-foundation/pact-go/v2
go get github.com/stretchr/testify
```
The provider verification logic is implemented in main_provider_test.go. This file contains:
* `MockPulseService`: A mock implementation of our service.PulseService interface. This mock is globally accessible within the test file, allowing Pact's state handlers to dynamically configure its behavior.
* `startServer()`: This function initializes our Gin application (SetupRouter) by injecting the MockPulseService. It then starts the application using httptest.NewServer, which provides a lightweight HTTP server running on a random port, ideal for testing.
* `createPulseStateHandler()`: A helper function that generates Pact models.StateHandler functions. These functions are invoked by the Pact verifier based on the "providerStates" defined in the consumer contract. Each state handler configures the MockPulseService to return specific data, simulating different scenarios (e.g., "Pulse is 1", "Pulse is 2").
* `TestV3HTTPProvider()`: This is the main test function that orchestrates the Pact verification process:
  * It starts the `startServer()` in a separate goroutine to run concurrently with the verifier.
  * It configures the `provider.Verifier` with essential details like the `ProviderBaseURL` (the address of our test server), the provider name, and the path to the Pact files (the consumer contracts).
  * `BeforeEach` and `AfterEach` hooks are used to reset the mock service's state, ensuring test isolation between interactions.
  * The `StateHandlers` map links the provider states from the contract to our `createPulseStateHandler` functions, enabling dynamic data provisioning for each interaction.

Execute all test (Integration and Pact Provider ones) with:
```bash
go test -v
```

## Reference Documentation
For further reference, please consider the following sections:
* [Go Programming Language](https://go.dev/doc/) 
* [Gin Web Framework Documentation](https://gin-gonic.com/en/docs/) 
* [Pact-Go Documentation](https://docs.pact.io/implementation_guides/go) 
* [Testify GitHub Repository](https://github.com/stretchr/testify)

## Guides

* [Go by Example](https://gobyexample.com/) - Hands-on introduction to Go.
* [Pact Contract Testing Tutorial](https://docs.pact.io/getting_started/how_pact_works) - Understanding how Pact works.
* [Pact-Go Provider Verification Example](https://github.com/pact-foundation/pact-go/blob/master/examples/provider_test.go) - Official Pact-Go Provider example.
