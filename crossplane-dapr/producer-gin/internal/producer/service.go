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
	requestLogger := loggerFromContext(ctx)

	event := OrderCreatedV1{ID: request.ID, Amount: request.Amount, EventVersion: "v1"}
	payload, err := json.Marshal(event)
	if err != nil {
		requestLogger.Error("failed to encode order event", "orderId", request.ID, "error", err)
		return fmt.Errorf("encode event: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, s.publishURL, bytes.NewBuffer(payload))
	if err != nil {
		requestLogger.Error("failed to create publish request", "orderId", request.ID, "error", err)
		return fmt.Errorf("create publish request: %w", err)
	}
	httpReq.Header.Set("Content-Type", "application/json")
	requestLogger.Debug("publishing order event", "orderId", request.ID, "url", s.publishURL)

	resp, err := s.httpClient.Do(httpReq)
	if err != nil {
		requestLogger.Error("publish request failed", "orderId", request.ID, "error", err)
		return fmt.Errorf("publish request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		requestLogger.Warn("publish endpoint returned non-2xx status", "orderId", request.ID, "statusCode", resp.StatusCode)
		return fmt.Errorf("publish endpoint returned status %d", resp.StatusCode)
	}

	requestLogger.Debug("publish request succeeded", "orderId", request.ID, "statusCode", resp.StatusCode)
	return nil
}
