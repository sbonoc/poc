package main

import (
	"log"

	"bono.poc/pact-provider-go/internal/handler"
	"bono.poc/pact-provider-go/internal/service"
	"github.com/gin-gonic/gin"
)

// SetupRouter configures and returns a Gin engine for the application.
// This function is made public to allow testing the router setup.
func SetupRouter(pulseService service.PulseService) *gin.Engine {

	_pulseService := pulseService
	// Initialize service layer if not provided
	if _pulseService == nil {
		_pulseService = service.NewPulseService()
	}

	// Initialize handler layer with its dependencies
	pulseHandler := handler.NewPulseHandler(_pulseService)

	// Create a new Gin router
	router := gin.Default() // gin.Default() includes Logger and Recovery middleware

	// Register routes
	router.GET("/api/pulses", pulseHandler.GetPulse)

	return router
}

func main() {
	router := SetupRouter(nil) // Pass nil to use default service initialization

	port := ":8080"
	log.Printf("Server starting on port %s", port)
	if err := router.Run(port); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	} else {
		log.Printf("Server started on port %s", port)
	}
}
