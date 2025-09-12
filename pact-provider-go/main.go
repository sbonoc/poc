package main

import (
	"log"

	"bono.poc/pact-provider-go/internal/handler"
	"bono.poc/pact-provider-go/internal/service"
	"github.com/gin-gonic/gin"
)

// SetupRouter configures and returns a Gin engine for the application.
// This function is made public to allow testing the router setup and to enable
// dependency injection of the PulseService for different environments (e.g., mocks for tests).
func SetupRouter(pulseService service.PulseService) *gin.Engine {

	_pulseService := pulseService
	// Initialize service layer if not provided (e.g., when called from main function).
	// This allows for dependency injection in tests by passing a mock service.
	if _pulseService == nil {
		_pulseService = service.NewPulseService()
	}

	// Initialize handler layer with its dependencies.
	// The handler depends on the PulseService to perform business logic.
	pulseHandler := handler.NewPulseHandler(_pulseService)

	// Create a new Gin router.
	// gin.Default() includes Logger and Recovery middleware by default,
	// which are useful for logging requests and handling panics.
	router := gin.Default()

	// Register routes for the application.
	// The GET /api/pulses endpoint is handled by the GetPulse method of pulseHandler.
	router.GET("/api/pulses", pulseHandler.GetPulse)

	return router
}

// main is the entry point of the application.
func main() {
	// Set up the router, passing nil for the PulseService to use the default
	// (real) implementation.
	router := SetupRouter(nil)

	// Define the port for the server to listen on.
	port := ":8080"
	log.Printf("Server starting on port %s", port)

	// Start the Gin server. router.Run() is a blocking call.
	if err := router.Run(port); err != nil {
		// Log a fatal error if the server fails to start.
		log.Fatalf("Server failed to start: %v", err)
	} else {
		// Log a message indicating successful server startup.
		log.Printf("Server started on port %s", port)
	}
}
