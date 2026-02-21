package consumer

import (
	"context"
	"encoding/json"
	"errors"
	"strings"
)

func ParseOrderEvent(ctx context.Context, payload []byte) (OrderCreatedV1, error) {
	requestLogger := loggerFromContext(ctx)

	var envelope CloudEventEnvelope
	if err := json.Unmarshal(payload, &envelope); err == nil && len(envelope.Data) > 0 {
		var event OrderCreatedV1
		if err := json.Unmarshal(envelope.Data, &event); err == nil {
			if event.EventVersion == "" {
				event.EventVersion = "v1"
			}
			if strings.TrimSpace(event.ID) == "" {
				requestLogger.Warn("parsed cloudevent without id")
				return OrderCreatedV1{}, errors.New("event id must not be blank")
			}
			requestLogger.Debug("parsed event as cloudevent", "id", event.ID, "version", event.EventVersion)
			return event, nil
		}
		requestLogger.Warn("failed to decode cloudevent payload, falling back to raw event", "error", err)
	}

	var rawEvent OrderCreatedV1
	if err := json.Unmarshal(payload, &rawEvent); err != nil {
		requestLogger.Error("failed to decode raw event payload", "error", err)
		return OrderCreatedV1{}, err
	}
	if rawEvent.EventVersion == "" {
		rawEvent.EventVersion = "v1"
	}
	if strings.TrimSpace(rawEvent.ID) == "" {
		requestLogger.Warn("parsed raw event without id")
		return OrderCreatedV1{}, errors.New("event id must not be blank")
	}
	requestLogger.Debug("parsed event as raw payload", "id", rawEvent.ID, "version", rawEvent.EventVersion)
	return rawEvent, nil
}
