package service

import (
	"fmt"
	"time"

	"bono.poc/pact-provider-go/internal/model"
)

// PulseService defines the interface for pulse-related operations.
// This interface abstracts the business logic for retrieving pulse data.
type PulseService interface {
	// GetPulse retrieves a pulse for a given date.
	// The `fromDate` parameter specifies the date for which to fetch the pulse.
	GetPulse(fromDate time.Time) (*model.Pulse, error)
}

// pulseServiceImpl is an implementation of the PulseService interface.
// It holds the actual business logic and any necessary dependencies.
type pulseServiceImpl struct {
	// Add any dependencies here, e.g., a database client,
	// to allow the service to interact with data sources.
	// For this example, it remains empty as it returns mock data.
}

// NewPulseService creates a new instance of PulseService.
// This is the constructor for the pulseServiceImpl, returning it as the PulseService interface.
func NewPulseService() PulseService {
	return &pulseServiceImpl{}
}

// GetPulse retrieves pulse data based on the 'fromDate' parameter.
// Currently, this implementation returns a mock Pulse object.
// In a real application, this would involve fetching data from a database or an external API.
func (s *pulseServiceImpl) GetPulse(fromDate time.Time) (*model.Pulse, error) {
	// Implement your data fetching logic here, e.g., querying a database or an external API.
	// For this example, we'll return a mock pulse with a default value.
	mockPulse := &model.Pulse{
		ID:        fmt.Sprintf("pulse-%d", fromDate.UnixNano()), // Unique ID based on the date
		Value:     1,                                            // Default pulse value
		CreatedAt: time.Now().UnixMilli(),                       // Current timestamp in milliseconds
		UpdatedAt: time.Now().UnixMilli(),                       // Current timestamp in milliseconds
		DeletedAt: 0,                                            // Indicates not deleted
	}

	return mockPulse, nil
}
