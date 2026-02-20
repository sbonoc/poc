package main

import (
	"log"

	"github.com/agnostic/crossplane-dapr/consumer-gin/internal/consumer"
	"github.com/prometheus/client_golang/prometheus"
)

func main() {
	cfg := consumer.LoadConfigFromEnv()
	router := consumer.NewRouter(cfg, prometheus.DefaultRegisterer, prometheus.DefaultGatherer)

	log.Printf("starting consumer-gin on :%s pubsub=%s topic=%s route=%s", cfg.Port, cfg.PubSubName, cfg.TopicName, cfg.SubscriptionRoute)
	if err := router.Run(":" + cfg.Port); err != nil {
		log.Fatalf("consumer-gin stopped with error: %v", err)
	}
}
