package consumer

import (
	"os"
	"strings"
)

type Config struct {
	Port              string
	PubSubName        string
	TopicName         string
	SubscriptionRoute string
}

func LoadConfigFromEnv() Config {
	return Config{
		Port:              envOrDefault("PORT", "8080"),
		PubSubName:        envOrDefault("DAPR_PUBSUB_NAME", "order-pubsub"),
		TopicName:         envOrDefault("DAPR_TOPIC_NAME", "orders"),
		SubscriptionRoute: NormalizeRoute(envOrDefault("DAPR_SUBSCRIPTION_ROUTE", "/orders")),
	}
}

func NormalizeRoute(route string) string {
	trimmed := strings.TrimSpace(route)
	if trimmed == "" {
		return "/orders"
	}
	if strings.HasPrefix(trimmed, "/") {
		return trimmed
	}
	return "/" + trimmed
}

func envOrDefault(key, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}
