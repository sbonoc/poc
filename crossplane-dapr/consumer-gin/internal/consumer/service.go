package consumer

import (
	"encoding/json"
	"errors"
	"strings"
)

func ParseOrderEvent(payload []byte) (OrderCreatedV1, error) {
	var envelope CloudEventEnvelope
	if err := json.Unmarshal(payload, &envelope); err == nil && len(envelope.Data) > 0 {
		var event OrderCreatedV1
		if err := json.Unmarshal(envelope.Data, &event); err == nil {
			if event.EventVersion == "" {
				event.EventVersion = "v1"
			}
			if strings.TrimSpace(event.ID) == "" {
				return OrderCreatedV1{}, errors.New("event id must not be blank")
			}
			return event, nil
		}
	}

	var rawEvent OrderCreatedV1
	if err := json.Unmarshal(payload, &rawEvent); err != nil {
		return OrderCreatedV1{}, err
	}
	if rawEvent.EventVersion == "" {
		rawEvent.EventVersion = "v1"
	}
	if strings.TrimSpace(rawEvent.ID) == "" {
		return OrderCreatedV1{}, errors.New("event id must not be blank")
	}
	return rawEvent, nil
}
