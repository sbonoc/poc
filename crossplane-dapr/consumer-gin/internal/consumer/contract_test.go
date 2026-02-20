//go:build contract

package consumer

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/pact-foundation/pact-go/dsl"
)

func contractProjectRoot(t *testing.T) string {
	t.Helper()
	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("failed to resolve test file path")
	}
	return filepath.Clean(filepath.Join(filepath.Dir(file), "..", ".."))
}

func TestConsumerGinPublishesPactForProducerGin(t *testing.T) {
	projectRoot := contractProjectRoot(t)
	pactDir := filepath.Join(projectRoot, "pacts")
	logDir := filepath.Join(projectRoot, "build", "pact-logs")

	if err := os.MkdirAll(pactDir, 0o755); err != nil {
		t.Fatalf("create pact dir: %v", err)
	}
	if err := os.MkdirAll(logDir, 0o755); err != nil {
		t.Fatalf("create pact log dir: %v", err)
	}

	pact := &dsl.Pact{
		Consumer:          "order-event-consumer-gin",
		Provider:          "order-event-producer-gin",
		PactDir:           pactDir,
		LogDir:            logDir,
		PactFileWriteMode: "overwrite",
	}
	defer pact.Teardown()

	pact.
		AddInteraction().
		Given("a valid order publish request").
		UponReceiving("a publish order request").
		WithRequest(dsl.Request{
			Method: http.MethodPost,
			Path:   dsl.String("/publish"),
			Headers: dsl.MapMatcher{
				"Content-Type": dsl.String("application/json"),
			},
			Body: dsl.MapMatcher{
				"id":     dsl.Like("ORD-GIN-300"),
				"amount": dsl.Like(55.1),
			},
		}).
		WillRespondWith(dsl.Response{
			Status: http.StatusAccepted,
			Headers: dsl.MapMatcher{
				"Content-Type": dsl.Regex("application/json; charset=utf-8", "application\\/json.*"),
			},
			Body: dsl.MapMatcher{
				"status":  dsl.Like("accepted"),
				"orderId": dsl.Like("ORD-GIN-300"),
			},
		})

	if err := pact.Verify(func() error {
		body := bytes.NewBufferString(`{"id":"ORD-GIN-300","amount":55.1}`)
		req, err := http.NewRequest(http.MethodPost, fmt.Sprintf("http://localhost:%d/publish", pact.Server.Port), body)
		if err != nil {
			return err
		}
		req.Header.Set("Content-Type", "application/json")

		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			return err
		}
		defer resp.Body.Close()

		if resp.StatusCode != http.StatusAccepted {
			return fmt.Errorf("expected status %d, got %d", http.StatusAccepted, resp.StatusCode)
		}

		var payload map[string]any
		if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
			return err
		}
		if payload["status"] != "accepted" {
			return fmt.Errorf("expected status=accepted, got %v", payload["status"])
		}
		if payload["orderId"] != "ORD-GIN-300" {
			return fmt.Errorf("expected orderId=ORD-GIN-300, got %v", payload["orderId"])
		}
		return nil
	}); err != nil {
		t.Fatalf("pact verify failed: %v", err)
	}
}
