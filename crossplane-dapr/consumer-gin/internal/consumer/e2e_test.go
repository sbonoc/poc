//go:build e2e

package consumer

import (
	"net/http"
	"os"
	"testing"
)

func TestE2EReadyEndpoint(t *testing.T) {
	if os.Getenv("RUN_E2E") != "true" {
		t.Skip("set RUN_E2E=true to execute e2e tests")
	}

	baseURL := os.Getenv("E2E_CONSUMER_GIN_BASE_URL")
	if baseURL == "" {
		baseURL = "http://localhost:8083"
	}

	resp, err := http.Get(baseURL + "/health/ready")
	if err != nil {
		t.Fatalf("request failed: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		t.Fatalf("expected 200, got %d", resp.StatusCode)
	}
}
