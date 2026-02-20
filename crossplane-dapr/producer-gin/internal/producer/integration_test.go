//go:build integration

package producer

import (
	"bytes"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/prometheus/client_golang/prometheus"
)

type doerFunc func(req *http.Request) (*http.Response, error)

func (d doerFunc) Do(req *http.Request) (*http.Response, error) { return d(req) }

func TestIntegrationPublishEndpoint(t *testing.T) {
	t.Parallel()

	doer := doerFunc(func(_ *http.Request) (*http.Response, error) {
		return &http.Response{
			StatusCode: http.StatusNoContent,
			Body:       io.NopCloser(bytes.NewBuffer(nil)),
		}, nil
	})

	service := NewService(doer, "http://dapr.local/publish")
	registry := prometheus.NewRegistry()
	router := NewRouter(Config{PubSubName: "order-pubsub", TopicName: "orders"}, service, registry, registry)

	req := httptest.NewRequest(http.MethodPost, "/publish", bytes.NewBufferString(`{"id":"ORD-10","amount":15}`))
	req.Header.Set("Content-Type", "application/json")
	res := httptest.NewRecorder()
	router.ServeHTTP(res, req)

	if res.Code != http.StatusAccepted {
		t.Fatalf("expected 202, got %d", res.Code)
	}

	metricsReq := httptest.NewRequest(http.MethodGet, "/metrics", nil)
	metricsRes := httptest.NewRecorder()
	router.ServeHTTP(metricsRes, metricsReq)

	if metricsRes.Code != http.StatusOK {
		t.Fatalf("metrics endpoint status = %d", metricsRes.Code)
	}
	if !bytes.Contains(metricsRes.Body.Bytes(), []byte("orders_publish_requests_total")) {
		t.Fatalf("expected publish metric in payload")
	}
}
