package main

import (
	"log"
	"net/http"
	"time"

	"github.com/agnostic/crossplane-dapr/producer-gin/internal/producer"
	"github.com/prometheus/client_golang/prometheus"
)

func main() {
	cfg := producer.LoadConfigFromEnv()
	client := &http.Client{Timeout: 5 * time.Second}
	service := producer.NewService(client, cfg.PublishURL())
	router := producer.NewRouter(cfg, service, prometheus.DefaultRegisterer, prometheus.DefaultGatherer)

	log.Printf("starting producer-gin on :%s pubsub=%s topic=%s", cfg.Port, cfg.PubSubName, cfg.TopicName)
	if err := router.Run(":" + cfg.Port); err != nil {
		log.Fatalf("producer-gin stopped with error: %v", err)
	}
}
