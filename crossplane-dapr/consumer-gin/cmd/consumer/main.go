package main

import (
	"log/slog"

	"github.com/agnostic/crossplane-dapr/consumer-gin/internal/consumer"
	"github.com/prometheus/client_golang/prometheus"
)

func main() {
	cfg := consumer.LoadConfigFromEnv()
	slog.SetDefault(consumer.Logger())
	router := consumer.NewRouter(cfg, prometheus.DefaultRegisterer, prometheus.DefaultGatherer)

	slog.Info("starting consumer-gin",
		"port", cfg.Port,
		"pubsub", cfg.PubSubName,
		"topic", cfg.TopicName,
		"route", cfg.SubscriptionRoute,
	)
	if err := router.Run(":" + cfg.Port); err != nil {
		slog.Error("consumer-gin stopped with error", "error", err)
		return
	}
	slog.Info("consumer-gin stopped")
}
