package consumer

import (
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func NewRouter(cfg Config, registerer prometheus.Registerer, gatherer prometheus.Gatherer) *gin.Engine {
	if registerer == nil {
		registerer = prometheus.DefaultRegisterer
	}
	if gatherer == nil {
		gatherer = prometheus.DefaultGatherer
	}

	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(gin.Logger(), gin.Recovery())

	consumedRequests := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "orders_consume_requests_total",
		Help: "Total consume requests received by consumer-gin.",
	})
	consumeErrors := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "orders_consume_errors_total",
		Help: "Total consume errors in consumer-gin.",
	})
	consumedEvents := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "orders_consumed_total",
		Help: "Total consumed order events in consumer-gin.",
	})
	httpRequestDuration := prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "http_server_requests_seconds",
			Help:    "HTTP request duration in seconds.",
			Buckets: prometheus.DefBuckets,
		},
		[]string{"method", "uri", "status"},
	)
	registerer.MustRegister(consumedRequests, consumeErrors, consumedEvents, httpRequestDuration)

	router.Use(func(c *gin.Context) {
		start := time.Now()
		c.Next()
		routeLabel := c.FullPath()
		if routeLabel == "" {
			routeLabel = c.Request.URL.Path
		}
		httpRequestDuration.WithLabelValues(
			c.Request.Method,
			routeLabel,
			strconv.Itoa(c.Writer.Status()),
		).Observe(time.Since(start).Seconds())
	})

	router.GET("/health/live", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})
	router.GET("/health/ready", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})
	router.GET("/metrics", gin.WrapH(promhttp.HandlerFor(gatherer, promhttp.HandlerOpts{})))

	router.GET("/dapr/subscribe", func(c *gin.Context) {
		subscriptions := []DaprSubscription{{
			PubSubName: cfg.PubSubName,
			Topic:      cfg.TopicName,
			Route:      cfg.SubscriptionRoute,
		}}
		c.JSON(http.StatusOK, subscriptions)
	})

	router.POST(cfg.SubscriptionRoute, func(c *gin.Context) {
		consumedRequests.Inc()

		payload, err := c.GetRawData()
		if err != nil {
			consumeErrors.Inc()
			c.JSON(http.StatusBadRequest, gin.H{"error": "failed to read event payload"})
			return
		}

		event, err := ParseOrderEvent(payload)
		if err != nil {
			consumeErrors.Inc()
			log.Printf("failed to parse event payload err=%v", err)
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid event payload"})
			return
		}

		consumedEvents.Inc()
		log.Printf("consumed order event route=%s id=%s version=%s", cfg.SubscriptionRoute, event.ID, event.EventVersion)
		c.Status(http.StatusOK)
	})

	return router
}
