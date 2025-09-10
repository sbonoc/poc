package model

type Pulse struct {
	ID        string `json:"id"`
	Value     int    `json:"value"`
	CreatedAt int64  `json:"createdAt"`
	UpdatedAt int64  `json:"updatedAt"`
	DeletedAt int64  `json:"deletedAt"`
}
