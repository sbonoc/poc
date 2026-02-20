package producer

import (
	"fmt"
	"os"
	"strings"
)

type Config struct {
	Port         string
	PubSubName   string
	TopicName    string
	DaprHTTPPort string
}

func LoadConfigFromEnv() Config {
	return Config{
		Port:         envOrDefault("PORT", "8080"),
		PubSubName:   envOrDefault("DAPR_PUBSUB_NAME", "order-pubsub"),
		TopicName:    envOrDefault("DAPR_TOPIC_NAME", "orders"),
		DaprHTTPPort: envOrDefault("DAPR_HTTP_PORT", "3500"),
	}
}

func (c Config) PublishURL() string {
	return fmt.Sprintf("http://localhost:%s/v1.0/publish/%s/%s", c.DaprHTTPPort, c.PubSubName, c.TopicName)
}

func envOrDefault(key, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}
