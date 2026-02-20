//go:build contract

package producer

import (
	"bytes"
	"io"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/pact-foundation/pact-go/dsl"
	"github.com/pact-foundation/pact-go/types"
	"github.com/prometheus/client_golang/prometheus"
)

type contractDoerFunc func(req *http.Request) (*http.Response, error)

func (d contractDoerFunc) Do(req *http.Request) (*http.Response, error) { return d(req) }

func providerProjectRoot(t *testing.T) string {
	t.Helper()
	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("failed to resolve test file path")
	}
	return filepath.Clean(filepath.Join(filepath.Dir(file), "..", ".."))
}

func TestProducerGinVerifiesConsumerPact(t *testing.T) {
	projectRoot := providerProjectRoot(t)
	pactFile := filepath.Join(projectRoot, "..", "consumer-gin", "pacts", "order-event-consumer-gin-order-event-producer-gin.json")

	doer := contractDoerFunc(func(_ *http.Request) (*http.Response, error) {
		return &http.Response{
			StatusCode: http.StatusNoContent,
			Body:       io.NopCloser(bytes.NewBuffer(nil)),
		}, nil
	})

	registry := prometheus.NewRegistry()
	service := NewService(doer, "http://dapr.local/publish")
	router := NewRouter(Config{}, service, registry, registry)
	server := httptest.NewServer(router)
	defer server.Close()

	pact := &dsl.Pact{}
	if _, err := pact.VerifyProvider(t, types.VerifyRequest{
		ProviderBaseURL: server.URL,
		PactURLs:        []string{pactFile},
	}); err != nil {
		t.Fatalf("provider pact verification failed: %v", err)
	}
}
