package handler_test // Use a different package name to avoid import cycles with 'handler'

import (
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"bono.poc/pact-provider-go/internal/handler" // Import the handler package
	"bono.poc/pact-provider-go/internal/model"
)

// MockPulseService implements service.PulseService for testing.
type MockPulseService struct {
	GetPulseFunc func(time.Time) (*model.Pulse, error)
}

// GetPulse is the mock implementation of the service.PulseService interface.
func (m *MockPulseService) GetPulse(fromDate time.Time) (*model.Pulse, error) {
	if m.GetPulseFunc != nil {
		return m.GetPulseFunc(fromDate)
	}
	return nil, errors.New("GetPulseFunc not set in mock")
}

func TestGetPulseHandler(t *testing.T) {
	// A fixed timestamp for CreatedAt/UpdatedAt in mock responses
	fixedMockTimestampMilli := int64(1672531200000) // Jan 1, 2023 00:00:00 UTC in milliseconds

	tests := []struct {
		name           string
		fromParam      string
		mockService    *MockPulseService // Mock for the service dependency
		expectedStatus int
		expectedBody   interface{} // Can be *model.Pulse or map[string]string
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
			fromParam: "",
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
					// Check if the date part matches
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
					log.Fatal("Service should not be called for invalid date format") // Fail if service is called
					return nil, nil
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
					return nil, errors.New("something went wrong in service")
				},
			},
			expectedStatus: http.StatusInternalServerError,
			expectedBody:   map[string]string{"error": "something went wrong in service"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Create a new handler with the mock service and mock time provider
			h := handler.NewPulseHandler(tt.mockService)

			req, err := http.NewRequest("GET", "/api/pulses", nil)
			if err != nil {
				t.Fatalf("could not create request: %v", err)
			}

			q := req.URL.Query()
			if tt.fromParam != "" {
				q.Add("from", tt.fromParam)
			}
			req.URL.RawQuery = q.Encode()

			rr := httptest.NewRecorder()

			h.GetPulse(rr, req)

			if rr.Code != tt.expectedStatus {
				t.Errorf("expected status %d; got %d", tt.expectedStatus, rr.Code)
			}

			// Compare response body
			if tt.expectedStatus == http.StatusOK {
				var actualPulse model.Pulse
				err := json.NewDecoder(rr.Body).Decode(&actualPulse)
				if err != nil {
					t.Fatalf("could not decode successful response: %v", err)
				}
				expectedPulse := tt.expectedBody.(*model.Pulse)
				if actualPulse.ID != expectedPulse.ID ||
					actualPulse.Value != expectedPulse.Value ||
					actualPulse.CreatedAt != expectedPulse.CreatedAt ||
					actualPulse.UpdatedAt != expectedPulse.UpdatedAt ||
					actualPulse.DeletedAt != expectedPulse.DeletedAt {
					t.Errorf("expected pulse %+v; got %+v", expectedPulse, actualPulse)
				}
			} else { // Error codes
				var actualErrorResponse map[string]string
				err := json.NewDecoder(rr.Body).Decode(&actualErrorResponse)
				if err != nil {
					t.Fatalf("could not decode error response: %v", err)
				}
				expectedErrorResponse := tt.expectedBody.(map[string]string)
				if actualErrorResponse["error"] != expectedErrorResponse["error"] {
					t.Errorf("expected error message '%s'; got '%s'", expectedErrorResponse["error"], actualErrorResponse["error"])
				}
			}
		})
	}
}
