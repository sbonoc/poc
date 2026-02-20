//go:build !integration && !contract && !e2e

package producer

import "testing"

func TestPublishOrderRequestValidate(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		request PublishOrderRequest
		wantErr bool
	}{
		{name: "valid", request: PublishOrderRequest{ID: "ORD-1", Amount: 10}, wantErr: false},
		{name: "blank id", request: PublishOrderRequest{ID: "  ", Amount: 10}, wantErr: true},
		{name: "zero amount", request: PublishOrderRequest{ID: "ORD-1", Amount: 0}, wantErr: true},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			err := tc.request.Validate()
			if tc.wantErr && err == nil {
				t.Fatalf("expected validation error")
			}
			if !tc.wantErr && err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
		})
	}
}

func TestPublishURL(t *testing.T) {
	t.Parallel()

	cfg := Config{DaprHTTPPort: "3500", PubSubName: "order-pubsub", TopicName: "orders"}
	if got := cfg.PublishURL(); got != "http://localhost:3500/v1.0/publish/order-pubsub/orders" {
		t.Fatalf("unexpected publish URL: %s", got)
	}
}
