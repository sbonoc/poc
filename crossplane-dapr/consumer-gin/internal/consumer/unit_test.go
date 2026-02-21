//go:build !integration && !contract && !e2e

package consumer

import (
	"context"
	"testing"
)

func TestNormalizeRoute(t *testing.T) {
	t.Parallel()

	cases := []struct {
		in   string
		want string
	}{
		{"/orders", "/orders"},
		{"orders", "/orders"},
		{"  ", "/orders"},
	}

	for _, tc := range cases {
		tc := tc
		t.Run(tc.in, func(t *testing.T) {
			t.Parallel()
			if got := NormalizeRoute(tc.in); got != tc.want {
				t.Fatalf("NormalizeRoute(%q)=%q, want %q", tc.in, got, tc.want)
			}
		})
	}
}

func TestParseOrderEvent(t *testing.T) {
	t.Parallel()

	event, err := ParseOrderEvent(context.Background(), []byte(`{"data":{"id":"ORD-1","amount":10,"eventVersion":"v1"}}`))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if event.ID != "ORD-1" || event.EventVersion != "v1" {
		t.Fatalf("unexpected event: %+v", event)
	}
}
