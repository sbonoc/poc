package producer

import (
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func NewRouter(cfg Config, service *Service, registerer prometheus.Registerer, gatherer prometheus.Gatherer) *gin.Engine {
	if registerer == nil {
		registerer = prometheus.DefaultRegisterer
	}
	if gatherer == nil {
		gatherer = prometheus.DefaultGatherer
	}

	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(gin.Logger(), gin.Recovery())

	publishRequests := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "orders_publish_requests_total",
		Help: "Total publish requests received by producer-gin.",
	})
	publishErrors := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "orders_publish_errors_total",
		Help: "Total publish errors in producer-gin.",
	})
	publishedEvents := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "orders_published_total",
		Help: "Total published order events from producer-gin.",
	})
	httpRequestDuration := prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "http_server_requests_seconds",
			Help:    "HTTP request duration in seconds.",
			Buckets: prometheus.DefBuckets,
		},
		[]string{"method", "uri", "status"},
	)
	registerer.MustRegister(publishRequests, publishErrors, publishedEvents, httpRequestDuration)

	router.Use(func(c *gin.Context) {
		start := time.Now()
		c.Next()
		route := c.FullPath()
		if route == "" {
			route = c.Request.URL.Path
		}
		httpRequestDuration.WithLabelValues(
			c.Request.Method,
			route,
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

	router.POST("/publish", func(c *gin.Context) {
		publishRequests.Inc()
		logger.Debug("received publish request")

		var req PublishOrderRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			publishErrors.Inc()
			logger.Warn("invalid publish request payload", "error", err)
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request payload"})
			return
		}
		if err := req.Validate(); err != nil {
			publishErrors.Inc()
			logger.Warn("publish validation failed", "orderId", req.ID, "error", err)
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		if err := service.Publish(c.Request.Context(), req); err != nil {
			publishErrors.Inc()
			logger.Error("publish failed", "orderId", req.ID, "error", err)
			c.JSON(http.StatusBadGateway, gin.H{"error": "failed to publish event"})
			return
		}

		publishedEvents.Inc()
		logger.Info("published order event", "id", req.ID, "version", "v1", "pubsub", cfg.PubSubName, "topic", cfg.TopicName)
		c.JSON(http.StatusAccepted, gin.H{"status": "accepted", "orderId": req.ID})
	})

	return router
}
