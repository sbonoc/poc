package handler

import (
	"net/http"
	"time"

	"bono.poc/pact-provider-go/internal/service"
	"github.com/gin-gonic/gin" // Import Gin
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
func (h *PulseHandler) GetPulse(c *gin.Context) {
	fromParam := c.Query("from")

	var fromDate time.Time
	var err error

	if fromParam == "" || fromParam == "today" {
		// For "today" or empty, use the current date (UTC start of day)
		now := time.Now().UTC()
		fromDate = time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, time.UTC)
	} else {
		// Attempt to parse as YYYY-MM-DD
		fromDate, err = time.Parse("2006-01-02", fromParam)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD)."})
			return
		}
		// Ensure the parsed date is also UTC start of day
		fromDate = time.Date(fromDate.Year(), fromDate.Month(), fromDate.Day(), 0, 0, 0, 0, time.UTC)
	}

	pulse, err := h.pulseService.GetPulse(fromDate)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, pulse)
}
