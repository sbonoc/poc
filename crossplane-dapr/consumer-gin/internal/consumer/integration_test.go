//go:build integration

package consumer

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/prometheus/client_golang/prometheus"
)

func TestIntegrationConsumeEndpoint(t *testing.T) {
	t.Parallel()

	cfg := Config{PubSubName: "order-pubsub", TopicName: "orders", SubscriptionRoute: "/orders"}
	registry := prometheus.NewRegistry()
	router := NewRouter(cfg, registry, registry)

	req := httptest.NewRequest(http.MethodPost, "/orders", bytes.NewBufferString(`{"data":{"id":"ORD-10","amount":12.3}}`))
	req.Header.Set("Content-Type", "application/json")
	res := httptest.NewRecorder()
	router.ServeHTTP(res, req)

	if res.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", res.Code)
	}

	metricsReq := httptest.NewRequest(http.MethodGet, "/metrics", nil)
	metricsRes := httptest.NewRecorder()
	router.ServeHTTP(metricsRes, metricsReq)
	if !bytes.Contains(metricsRes.Body.Bytes(), []byte("orders_consume_requests_total")) {
		t.Fatalf("expected consume metric in payload")
	}
}
