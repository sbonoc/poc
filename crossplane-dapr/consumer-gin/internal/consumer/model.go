package consumer

import "encoding/json"

type DaprSubscription struct {
	PubSubName string `json:"pubsubname"`
	Topic      string `json:"topic"`
	Route      string `json:"route"`
}

type CloudEventEnvelope struct {
	Data json.RawMessage `json:"data"`
}

type OrderCreatedV1 struct {
	ID           string  `json:"id"`
	Amount       float64 `json:"amount"`
	EventVersion string  `json:"eventVersion"`
}
