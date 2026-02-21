package main

import (
	"log/slog"
	"net/http"
	"time"

	"github.com/agnostic/crossplane-dapr/producer-gin/internal/producer"
	"github.com/prometheus/client_golang/prometheus"
)

func main() {
	cfg := producer.LoadConfigFromEnv()
	slog.SetDefault(producer.Logger())
	client := &http.Client{Timeout: 5 * time.Second}
	service := producer.NewService(client, cfg.PublishURL())
	router := producer.NewRouter(cfg, service, prometheus.DefaultRegisterer, prometheus.DefaultGatherer)

	slog.Info("starting producer-gin",
		"port", cfg.Port,
		"pubsub", cfg.PubSubName,
		"topic", cfg.TopicName,
		"daprHttpPort", cfg.DaprHTTPPort,
	)
	if err := router.Run(":" + cfg.Port); err != nil {
		slog.Error("producer-gin stopped with error", "error", err)
		return
	}
	slog.Info("producer-gin stopped")
}
