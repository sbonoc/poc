package model

// Pulse represents a single pulse record in the system.
// It defines the structure of the data that will be stored and transferred.
type Pulse struct {
	// ID is the unique identifier for the pulse.
	ID string `json:"id"`
	// Value represents the numerical value of the pulse.
	Value int `json:"value"`
	// CreatedAt stores the Unix timestamp (in milliseconds) when the pulse record was created.
	CreatedAt int64 `json:"createdAt"`
	// UpdatedAt stores the Unix timestamp (in milliseconds) when the pulse record was last updated.
	UpdatedAt int64 `json:"updatedAt"`
	// DeletedAt stores the Unix timestamp (in milliseconds) when the pulse record was deleted.
	// A value of 0 typically indicates that the record is not deleted.
	DeletedAt int64 `json:"deletedAt"`
}
