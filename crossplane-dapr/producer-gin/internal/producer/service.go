package producer

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
)

type HTTPDoer interface {
	Do(req *http.Request) (*http.Response, error)
}

type Service struct {
	httpClient HTTPDoer
	publishURL string
}

func NewService(httpClient HTTPDoer, publishURL string) *Service {
	return &Service{httpClient: httpClient, publishURL: publishURL}
}

func (s *Service) Publish(ctx context.Context, request PublishOrderRequest) error {
	event := OrderCreatedV1{ID: request.ID, Amount: request.Amount, EventVersion: "v1"}
	payload, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("encode event: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, s.publishURL, bytes.NewBuffer(payload))
	if err != nil {
		return fmt.Errorf("create publish request: %w", err)
	}
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(httpReq)
	if err != nil {
		return fmt.Errorf("publish request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return fmt.Errorf("publish endpoint returned status %d", resp.StatusCode)
	}

	return nil
}
