package handler

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"bono.poc/pact-provider-go/internal/service"
)

// PulseHandler handles HTTP requests related to pulses.
type PulseHandler struct {
	pulseService service.PulseService
	now          func() time.Time // Dependency for getting the current time
}

// NewPulseHandler creates a new instance of PulseHandler.
// It takes a PulseService and an optional time function.
// If no time function is provided, it defaults to time.Now.
func NewPulseHandler(ps service.PulseService) *PulseHandler {
	return &PulseHandler{
		pulseService: ps,
		now:          time.Now, // Default to time.Now
	}
}

// GetPulse handles GET requests to /api/pulses.
func (h *PulseHandler) GetPulse(w http.ResponseWriter, r *http.Request) {
	fromParam := r.URL.Query().Get("from")
	log.Printf("Received GET request for pulses from '%s'", fromParam)

	var fromDate time.Time
	var err error

	// Parse 'from' parameter to time.Time
	switch fromParam {
	case "today":
		fromDate = h.now() // Use the injected time function
	case "":
		fromDate = h.now() // Default to current time if 'from' is not provided
	default:
		fromDate, err = time.Parse("2006-01-02", fromParam)
		if err != nil {
			sendErrorResponse(w, "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD).", http.StatusBadRequest)
			return
		}
	}

	pulse, err := h.pulseService.GetPulse(fromDate)
	if err != nil {
		log.Printf("Error getting pulse from service: %v", err)
		// A generic service error is typically 500. Adjust as per your API design.
		sendErrorResponse(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Only set 200 OK and encode if no error occurred
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(pulse); err != nil {
		log.Printf("Error encoding response: %v", err)
		sendErrorResponse(w, "Internal Server Error", http.StatusInternalServerError)
		return // Ensure to return after sending error
	}
	log.Printf("Successfully returned pulse for fromParam '%s'", fromParam)
}

// sendErrorResponse is a helper function to send JSON error responses.
func sendErrorResponse(w http.ResponseWriter, message string, statusCode int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	err := json.NewEncoder(w).Encode(map[string]string{"error": message})
	if err != nil {
		log.Fatalf("Error encoding error response: %v", err)
		return
	}
}
