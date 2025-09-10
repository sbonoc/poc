package service

import (
	"fmt"
	"time"

	"bono.poc/pact-provider-go/internal/model"
)

// PulseService defines the interface for pulse-related operations.
type PulseService interface {
	GetPulse(fromParam time.Time) (*model.Pulse, error) // Changed parameter type to time.Time
}

// pulseServiceImpl is an implementation of PulseService.
type pulseServiceImpl struct {
	// Add any dependencies here, e.g., a database client
}

// NewPulseService creates a new instance of PulseService.
func NewPulseService() PulseService {
	return &pulseServiceImpl{}
}

// GetPulse retrieves pulse data based on the 'from' parameter.
func (s *pulseServiceImpl) GetPulse(fromDate time.Time) (*model.Pulse, error) { // Changed parameter type to time.Time
	mockPulse := &model.Pulse{
		ID:        fmt.Sprintf("pulse-%d", fromDate.UnixNano()),
		Value:     1, // Default value
		CreatedAt: time.Now().UnixMilli(),
		UpdatedAt: time.Now().UnixMilli(),
		DeletedAt: 0,
	}

	return mockPulse, nil
}
