package main

import (
	"log"
	"net/http"

	"bono.poc/pact-provider-go/internal/handler"
	"bono.poc/pact-provider-go/internal/service"
)

func main() {
	// Initialize service layer
	pulseService := service.NewPulseService()

	// Initialize handler layer with its dependencies
	pulseHandler := handler.NewPulseHandler(pulseService)

	// Register routes
	http.HandleFunc("/api/pulses", pulseHandler.GetPulse)

	port := ":8080"
	log.Printf("Server starting on port %s", port)
	if err := http.ListenAndServe(port, nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
