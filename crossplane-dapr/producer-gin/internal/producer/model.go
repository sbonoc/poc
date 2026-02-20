package producer

import (
	"errors"
	"strings"
)

type PublishOrderRequest struct {
	ID     string  `json:"id"`
	Amount float64 `json:"amount"`
}

type OrderCreatedV1 struct {
	ID           string  `json:"id"`
	Amount       float64 `json:"amount"`
	EventVersion string  `json:"eventVersion"`
}

func (r PublishOrderRequest) Validate() error {
	if strings.TrimSpace(r.ID) == "" {
		return errors.New("id must not be blank")
	}
	if r.Amount <= 0 {
		return errors.New("amount must be greater than zero")
	}
	return nil
}
