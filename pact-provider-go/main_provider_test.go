package main

import (
	"fmt"
	"os"
	"path/filepath"
	"testing"
	"time"

	"net/http/httptest" // httptest provides utilities for HTTP testing

	"bono.poc/pact-provider-go/internal/model"
	_ "bono.poc/pact-provider-go/internal/service" // Import service package for its interface, but not directly used here
	// The blank import is often used to ensure that the package's init() function is called,
	// or to satisfy a dependency for an interface without directly using its types.

	"github.com/pact-foundation/pact-go/v2/models"   // Pact models for provider states
	"github.com/pact-foundation/pact-go/v2/provider" // Pact provider verification library
	"github.com/stretchr/testify/assert"             // Assertion library for testing
)

// Get the current working directory to construct paths.
var dir, _ = os.Getwd()

// Define the directory where Pact files (consumer contracts) are stored.
var pactDir = fmt.Sprintf("%s/pacts", dir)

// MockPulseService implements service.PulseService for testing.
// This mock allows us to control the behavior of the service layer during provider tests.
type MockPulseService struct {
	// GetPulseFunc is a field that can be set to a function, allowing dynamic
	// definition of the mock's behavior for the GetPulse method.
	GetPulseFunc func(time.Time) (*model.Pulse, error)
}

// GetPulse is the mock implementation of the service.PulseService interface.
// If GetPulseFunc is set, it executes that function; otherwise, it provides a default mock response.
func (m *MockPulseService) GetPulse(fromDate time.Time) (*model.Pulse, error) {
	if m.GetPulseFunc != nil {
		return m.GetPulseFunc(fromDate)
	}
	// Default mock behavior if GetPulseFunc is not explicitly set for a state.
	return &model.Pulse{
		ID:        "mock-pulse-id-default",
		Value:     100,
		CreatedAt: time.Now().UnixMilli(),
		UpdatedAt: time.Now().UnixMilli(),
		DeletedAt: 0,
	}, nil
}

// Global variables to hold the test server instance and the mock service.
// These are global so that state handlers can modify the mock service's behavior.
var testServer *httptest.Server
var mockPulseService *MockPulseService // Global mock to be able to change its behavior in state handlers

// createPulseStateHandler is a helper function to generate Pact provider state handlers.
// It returns a models.StateHandler function that configures the mock service based on the given pulseValue.
func createPulseStateHandler(t *testing.T, pulseValue int) models.StateHandler {
	return func(setup bool, s models.ProviderState) (models.ProviderStateResponse, error) {
		if setup {
			// This block runs when the provider state needs to be set up.
			t.Logf("[DEBUG] HOOK calling 'Pulse is %d' state handler %v", pulseValue, s)
			// Configure the global mock service for this specific state.
			mockPulseService.GetPulseFunc = func(fromDate time.Time) (*model.Pulse, error) {
				t.Logf("[DEBUG] MockPulseService.GetPulse called for state 'Pulse is %d' with date: %v", pulseValue, fromDate)
				// Use a fixed timestamp for consistency in tests.
				fixedMockTimestampMilli := int64(1672531200000) // Jan 1, 2023 00:00:00 UTC in milliseconds
				return &model.Pulse{
					ID:        "12345",
					Value:     pulseValue, // Set the pulse value according to the state
					CreatedAt: fixedMockTimestampMilli,
					UpdatedAt: fixedMockTimestampMilli,
					DeletedAt: 0,
				}, nil
			}
		} else {
			// This block runs when the provider state needs to be torn down (after the interaction).
			t.Logf("[DEBUG] HOOK teardown the 'Pulse is %d' state", pulseValue)
			// In this simple case, no specific teardown is needed as BeforeEach resets the mock.
		}

		return nil, nil
	}
}

// TestV3HTTPProvider runs the Pact provider verification process.
func TestV3HTTPProvider(t *testing.T) {
	// Start the provider API in a separate goroutine so it runs concurrently with the verifier.
	go startServer()

	// Give the server a moment to start up and assign to testServer.
	time.Sleep(100 * time.Millisecond)

	// Ensure the test server is closed after the test finishes.
	defer func() {
		if testServer != nil {
			testServer.Close()
			t.Log("Test server closed.")
		}
	}()

	// Create a new Pact verifier instance.
	verifier := provider.NewVerifier()

	// Configure and run the provider verification.
	err := verifier.VerifyProvider(t, provider.VerifyRequest{
		ProviderBaseURL: testServer.URL, // The URL of the running test server.
		Provider:        "superapp-api", // The name of the provider as defined in the Pact contract.
		PactFiles: []string{
			// Specify the path to the Pact contract file(s).
			filepath.ToSlash(fmt.Sprintf("%s/superapp-ui-superapp-api.json", pactDir)),
		},
		BeforeEach: func() error {
			// This hook runs before each interaction is verified.
			t.Log("[DEBUG] HOOK before each")
			// Reset the mock service's behavior to its default (or nil) state
			// to ensure test isolation between interactions.
			mockPulseService.GetPulseFunc = nil
			return nil
		},
		AfterEach: func() error {
			// This hook runs after each interaction is verified.
			t.Log("[DEBUG] HOOK after each")
			return nil
		},
		// Define the state handlers that Pact will call based on the "providerStates"
		// defined in the consumer contract.
		StateHandlers: models.StateHandlers{
			"Pulse is 1": createPulseStateHandler(t, 1),
			"Pulse is 2": createPulseStateHandler(t, 2),
		},
		DisableColoredOutput: false, // Disable colored output for cleaner logs in some CI environments, now enabled.
	})

	// Assert that no error occurred during the verification process.
	assert.NoError(t, err)
}

// startServer initializes and starts the Gin HTTP server for testing.
func startServer() {
	// Initialize the global mock service.
	mockPulseService = &MockPulseService{}

	// Setup the Gin router, injecting the mock service.
	// SetupRouter returns a *gin.Engine, which implements http.Handler.
	router := SetupRouter(mockPulseService)

	// Create a new test HTTP server using httptest.NewServer.
	// This server listens on a random available port.
	testServer = httptest.NewServer(router)
}
