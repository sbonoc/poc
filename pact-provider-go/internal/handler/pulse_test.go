package handler_test // Use a different package name to avoid import cycles with 'handler'

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"bono.poc/pact-provider-go/internal/handler"
	"bono.poc/pact-provider-go/internal/model"
	"github.com/gin-gonic/gin"
)

// MockPulseService implements service.PulseService for testing purposes.
// It allows us to control the behavior of the service layer during handler tests.
type MockPulseService struct {
	// GetPulseFunc is a function field that can be set to define the mock's behavior
	// for the GetPulse method. This enables dynamic mocking for different test cases.
	GetPulseFunc func(time.Time) (*model.Pulse, error)
}

// GetPulse is the mock implementation of the service.PulseService interface.
// If GetPulseFunc is set, it calls that function; otherwise, it returns an error
// indicating that the mock behavior was not defined.
func (m *MockPulseService) GetPulse(fromDate time.Time) (*model.Pulse, error) {
	if m.GetPulseFunc != nil {
		return m.GetPulseFunc(fromDate)
	}
	return nil, errors.New("GetPulseFunc not set in mock")
}

// TestGetPulseHandler contains unit tests for the GetPulse HTTP handler.
func TestGetPulseHandler(t *testing.T) {
	// A fixed timestamp for CreatedAt/UpdatedAt in mock responses to ensure consistent test results.
	fixedMockTimestampMilli := int64(1672531200000) // Jan 1, 2023 00:00:00 UTC in milliseconds

	// Define a slice of test cases, each describing a specific scenario for the GetPulse handler.
	tests := []struct {
		name           string            // Name of the test case
		fromParam      string            // Value of the 'from' query parameter
		mockService    *MockPulseService // Mock service configured for this test case
		expectedStatus int               // Expected HTTP status code
		expectedBody   interface{}       // Expected response body (can be *model.Pulse or map[string]string for errors)
	}{
		{
			name:      "GET /api/pulses?from=today - success",
			fromParam: "today",
			mockService: &MockPulseService{
				GetPulseFunc: func(t time.Time) (*model.Pulse, error) {
					return &model.Pulse{
						ID:        "mock-pulse-id-1",
						Value:     1,
						CreatedAt: fixedMockTimestampMilli,
						UpdatedAt: fixedMockTimestampMilli,
						DeletedAt: 0,
					}, nil
				},
			},
			expectedStatus: http.StatusOK,
			expectedBody: &model.Pulse{
				ID:        "mock-pulse-id-1",
				Value:     1,
				CreatedAt: fixedMockTimestampMilli,
				UpdatedAt: fixedMockTimestampMilli,
				DeletedAt: 0,
			},
		},
		{
			name:      "GET /api/pulses (empty from) - success",
			fromParam: "", // Empty 'from' parameter should default to 'today'
			mockService: &MockPulseService{
				GetPulseFunc: func(t time.Time) (*model.Pulse, error) {
					return &model.Pulse{
						ID:        "mock-pulse-id-2",
						Value:     2,
						CreatedAt: fixedMockTimestampMilli,
						UpdatedAt: fixedMockTimestampMilli,
						DeletedAt: 0,
					}, nil
				},
			},
			expectedStatus: http.StatusOK,
			expectedBody: &model.Pulse{
				ID:        "mock-pulse-id-2",
				Value:     2,
				CreatedAt: fixedMockTimestampMilli,
				UpdatedAt: fixedMockTimestampMilli,
				DeletedAt: 0,
			},
		},
		{
			name:      "GET /api/pulses?from=2023-01-15 - success",
			fromParam: "2023-01-15",
			mockService: &MockPulseService{
				GetPulseFunc: func(t time.Time) (*model.Pulse, error) {
					// Verify that the service's GetPulse method is called with the correct date.
					expectedDate := time.Date(2023, time.January, 15, 0, 0, 0, 0, time.UTC)
					if t.Year() != expectedDate.Year() || t.Month() != expectedDate.Month() || t.Day() != expectedDate.Day() {
						return nil, fmt.Errorf("expected service to be called with date %v, got %v", expectedDate, t)
					}
					return &model.Pulse{
						ID:        "mock-pulse-id-3",
						Value:     10,
						CreatedAt: fixedMockTimestampMilli,
						UpdatedAt: fixedMockTimestampMilli,
						DeletedAt: 0,
					}, nil
				},
			},
			expectedStatus: http.StatusOK,
			expectedBody: &model.Pulse{
				ID:        "mock-pulse-id-3",
				Value:     10,
				CreatedAt: fixedMockTimestampMilli,
				UpdatedAt: fixedMockTimestampMilli,
				DeletedAt: 0,
			},
		},
		{
			name:      "GET /api/pulses?from=invalid-date - bad request",
			fromParam: "invalid-date",
			mockService: &MockPulseService{
				GetPulseFunc: func(t time.Time) (*model.Pulse, error) {
					// This mock function should ideally not be called if the handler correctly
					// validates the 'from' parameter before calling the service.
					return nil, errors.New("service should not be called for invalid date format")
				},
			},
			expectedStatus: http.StatusBadRequest,
			expectedBody:   map[string]string{"error": "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD)."},
		},
		{
			name:      "Service returns an error - internal server error",
			fromParam: "today",
			mockService: &MockPulseService{
				GetPulseFunc: func(t time.Time) (*model.Pulse, error) {
					// Simulate an error from the service layer.
					return nil, errors.New("something went wrong in service")
				},
			},
			expectedStatus: http.StatusInternalServerError,
			expectedBody:   map[string]string{"error": "something went wrong in service"},
		},
	}

	// Iterate over each test case.
	for _, tt := range tests {
		// Run each test case as a subtest to isolate failures.
		t.Run(tt.name, func(t *testing.T) {
			// Set Gin to Test Mode to suppress console output during tests.
			gin.SetMode(gin.TestMode)

			// Create a new PulseHandler instance, injecting the mock service.
			pulseHandler := handler.NewPulseHandler(tt.mockService)

			// Create a new Gin router. Using gin.New() provides a router with minimal middleware,
			// which is often preferred for unit testing handlers.
			router := gin.New()
			// Register the GetPulse handler for the /api/pulses GET route.
			router.GET("/api/pulses", pulseHandler.GetPulse)

			// Construct the request URL, including the 'from' query parameter if provided.
			url := "/api/pulses"
			if tt.fromParam != "" {
				url = fmt.Sprintf("%s?from=%s", url, tt.fromParam)
			}

			// Create a new HTTP GET request.
			req, err := http.NewRequest("GET", url, nil)
			if err != nil {
				t.Fatalf("could not create request: %v", err)
			}

			// Create a ResponseRecorder to capture the HTTP response.
			rr := httptest.NewRecorder()
			// Serve the HTTP request using the Gin router.
			router.ServeHTTP(rr, req)

			// Assert the HTTP status code.
			if rr.Code != tt.expectedStatus {
				t.Errorf("expected status %d; got %d", tt.expectedStatus, rr.Code)
			}

			// Compare the response body based on whether it's a success or an error.
			if tt.expectedStatus == http.StatusOK {
				var actualPulse model.Pulse
				// Decode the JSON response body into a Pulse model.
				err := json.NewDecoder(rr.Body).Decode(&actualPulse)
				if err != nil {
					t.Fatalf("could not decode successful response: %v", err)
				}
				// Type assert the expected body to a *model.Pulse for comparison.
				expectedPulse := tt.expectedBody.(*model.Pulse)
				// Compare individual fields of the Pulse objects.
				if actualPulse.ID != expectedPulse.ID ||
					actualPulse.Value != expectedPulse.Value ||
					actualPulse.CreatedAt != expectedPulse.CreatedAt ||
					actualPulse.UpdatedAt != expectedPulse.UpdatedAt ||
					actualPulse.DeletedAt != expectedPulse.DeletedAt {
					t.Errorf("expected pulse %+v; got %+v", expectedPulse, actualPulse)
				}
			} else { // Handle error responses
				var actualErrorResponse map[string]string
				// Decode the JSON error response into a map.
				err := json.NewDecoder(rr.Body).Decode(&actualErrorResponse)
				if err != nil {
					t.Fatalf("could not decode error response: %v", err)
				}
				// Type assert the expected body to a map[string]string for comparison.
				expectedErrorResponse := tt.expectedBody.(map[string]string)
				// Compare the error message.
				if actualErrorResponse["error"] != expectedErrorResponse["error"] {
					t.Errorf("expected error message '%s'; got '%s'", expectedErrorResponse["error"], actualErrorResponse["error"])
				}
			}
		})
	}
}
