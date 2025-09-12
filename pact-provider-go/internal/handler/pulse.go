package handler

import (
	"net/http"
	"time"

	"bono.poc/pact-provider-go/internal/service"
	"github.com/gin-gonic/gin"
)

// PulseHandler handles HTTP requests related to pulse data.
type PulseHandler struct {
	pulseService service.PulseService
}

// NewPulseHandler creates a new instance of PulseHandler.
func NewPulseHandler(s service.PulseService) *PulseHandler {
	return &PulseHandler{
		pulseService: s,
	}
}

// GetPulse handles the GET /api/pulses request.
// It retrieves pulse data based on the 'from' query parameter.
// The 'from' parameter can be "today" or a date in "YYYY-MM-DD" format.
func (h *PulseHandler) GetPulse(c *gin.Context) {
	// Extract the 'from' query parameter from the request.
	fromParam := c.Query("from")

	var fromDate time.Time
	var err error

	// Determine the date to query based on the 'from' parameter.
	if fromParam == "" || fromParam == "today" {
		// If 'from' is empty or "today", use the current date.
		// Set the time to the start of the day (00:00:00) in UTC to ensure consistency.
		now := time.Now().UTC()
		fromDate = time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, time.UTC)
	} else {
		// Attempt to parse the 'from' parameter as a date in "YYYY-MM-DD" format.
		fromDate, err = time.Parse("2006-01-02", fromParam)
		if err != nil {
			// If parsing fails, return a 400 Bad Request error.
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD)."})
			return
		}
		// Ensure the parsed date is also set to the start of the day (00:00:00) in UTC.
		fromDate = time.Date(fromDate.Year(), fromDate.Month(), fromDate.Day(), 0, 0, 0, 0, time.UTC)
	}

	// Call the pulse service to get the pulse data for the determined date.
	pulse, err := h.pulseService.GetPulse(fromDate)
	if err != nil {
		// If an error occurs during service call, return a 500 Internal Server Error.
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// If successful, return the pulse data with a 200 OK status.
	c.JSON(http.StatusOK, pulse)
}
