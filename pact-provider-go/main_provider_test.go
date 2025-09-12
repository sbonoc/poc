package main

import (
	"fmt"
	l "log"
	"os"
	"path/filepath"
	"testing"
	"time"

	"net/http/httptest"

	"bono.poc/pact-provider-go/internal/model"
	_ "bono.poc/pact-provider-go/internal/service"

	"github.com/pact-foundation/pact-go/v2/models"
	"github.com/pact-foundation/pact-go/v2/provider"
	"github.com/stretchr/testify/assert"
)

var dir, _ = os.Getwd()
var pactDir = fmt.Sprintf("%s/pacts", dir)

var stateHandlerCalled = false

// MockPulseService implements service.PulseService for testing.
type MockPulseService struct {
	GetPulseFunc func(time.Time) (*model.Pulse, error)
}

// GetPulse is the mock implementation of the service.PulseService interface.
func (m *MockPulseService) GetPulse(fromDate time.Time) (*model.Pulse, error) {
	if m.GetPulseFunc != nil {
		return m.GetPulseFunc(fromDate)
	}
	// Default mock behavior if GetPulseFunc is not set
	l.Println("[DEBUG] MockPulseService.GetPulse called with default behavior.")
	return &model.Pulse{
		ID:        "mock-pulse-id-default",
		Value:     100,
		CreatedAt: time.Now().UnixMilli(),
		UpdatedAt: time.Now().UnixMilli(),
		DeletedAt: 0,
	}, nil
}

var testServer *httptest.Server
var mockPulseService *MockPulseService // Global mock to be able to change its behavior in state handlers

// createPulseStateHandler is a helper function to generate state handlers for different pulse values.
func createPulseStateHandler(pulseValue int) models.StateHandler {
	return func(setup bool, s models.ProviderState) (models.ProviderStateResponse, error) {
		stateHandlerCalled = true

		if setup {
			l.Printf("[DEBUG] HOOK calling 'Pulse is %d' state handler %v", pulseValue, s)
			// Configure the mock service for this specific state
			mockPulseService.GetPulseFunc = func(fromDate time.Time) (*model.Pulse, error) {
				l.Printf("[DEBUG] MockPulseService.GetPulse called for state 'Pulse is %d' with date: %v", pulseValue, fromDate)
				fixedMockTimestampMilli := int64(1672531200000) // Jan 1, 2023 00:00:00 UTC in milliseconds
				return &model.Pulse{
					ID:        "12345",
					Value:     pulseValue,
					CreatedAt: fixedMockTimestampMilli,
					UpdatedAt: fixedMockTimestampMilli,
					DeletedAt: 0,
				}, nil
			}
		} else {
			l.Printf("[DEBUG] HOOK teardown the 'Pulse is %d' state", pulseValue)
		}

		return models.ProviderStateResponse{"uuid": "1234"}, nil
	}
}

func TestV3HTTPProvider(t *testing.T) {
	// Start provider API in the background
	go startServer()

	// Wait for the server to start and be assigned to testServer
	time.Sleep(100 * time.Millisecond) // Give server a moment to start

	defer func() {
		if testServer != nil {
			testServer.Close()
			l.Println("Test server closed.")
		}
	}()

	verifier := provider.NewVerifier()

	err := verifier.VerifyProvider(t, provider.VerifyRequest{
		ProviderBaseURL: testServer.URL, // Use the URL from the test server
		Provider:        "superapp-api", // Changed from V3Provider to match the pact file name
		PactFiles: []string{
			filepath.ToSlash(fmt.Sprintf("%s/superapp-ui-superapp-api.json", pactDir)), // Use the correct pact file name
		},
		BeforeEach: func() error {
			l.Println("[DEBUG] HOOK before each")
			// Reset mock behavior before each interaction if needed
			mockPulseService.GetPulseFunc = nil // Reset to default behavior
			return nil
		},
		AfterEach: func() error {
			l.Println("[DEBUG] HOOK after each")
			return nil
		},
		StateHandlers: models.StateHandlers{
			"Pulse is 1": createPulseStateHandler(1),
			"Pulse is 2": createPulseStateHandler(2),
		},
		DisableColoredOutput: true,
	})
	assert.NoError(t, err)
	assert.True(t, stateHandlerCalled, "State handler not called as expected")
}

func startServer() {
	// Initialize a mock service
	mockPulseService = &MockPulseService{}

	// Setup the router with the mock service
	// main.SetupRouter now returns *gin.Engine, which implements http.Handler
	router := SetupRouter(mockPulseService)

	// Start the test server
	testServer = httptest.NewServer(router)
	l.Printf("Pact Provider Test: Application server running on: %s", testServer.URL)
}
