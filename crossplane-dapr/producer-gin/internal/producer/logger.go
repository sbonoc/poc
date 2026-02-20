package producer

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
)

var logger = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
	Level: resolveLogLevel(),
}))

const requestLoggerKey = "requestLogger"

type contextLoggerKey struct{}

func resolveLogLevel() slog.Level {
	switch strings.ToUpper(strings.TrimSpace(os.Getenv("LOG_LEVEL"))) {
	case "DEBUG":
		return slog.LevelDebug
	case "WARN", "WARNING":
		return slog.LevelWarn
	case "ERROR":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}

func loggerFromGinContext(c *gin.Context) *slog.Logger {
	if value, ok := c.Get(requestLoggerKey); ok {
		if requestLogger, ok := value.(*slog.Logger); ok {
			return requestLogger
		}
	}
	return logger
}

func withRequestLogger(ctx context.Context, requestLogger *slog.Logger) context.Context {
	return context.WithValue(ctx, contextLoggerKey{}, requestLogger)
}

func loggerFromContext(ctx context.Context) *slog.Logger {
	if value := ctx.Value(contextLoggerKey{}); value != nil {
		if requestLogger, ok := value.(*slog.Logger); ok {
			return requestLogger
		}
	}
	return logger
}

func requestTraceContext(r *http.Request) (string, string) {
	if traceID, spanID, ok := parseTraceparent(r.Header.Get("traceparent")); ok {
		return traceID, spanID
	}

	traceID := strings.TrimSpace(r.Header.Get("x-b3-traceid"))
	if traceID == "" {
		traceID = strings.TrimSpace(r.Header.Get("x-request-id"))
	}
	if traceID == "" {
		traceID = "unknown"
	}

	spanID := strings.TrimSpace(r.Header.Get("x-b3-spanid"))
	if spanID == "" {
		spanID = "unknown"
	}

	return strings.ToLower(traceID), strings.ToLower(spanID)
}

func parseTraceparent(header string) (string, string, bool) {
	parts := strings.Split(strings.TrimSpace(header), "-")
	if len(parts) != 4 {
		return "", "", false
	}

	traceID := parts[1]
	spanID := parts[2]
	if len(traceID) != 32 || len(spanID) != 16 || !isHex(traceID) || !isHex(spanID) {
		return "", "", false
	}
	if traceID == strings.Repeat("0", 32) || spanID == strings.Repeat("0", 16) {
		return "", "", false
	}

	return strings.ToLower(traceID), strings.ToLower(spanID), true
}

func isHex(value string) bool {
	for _, char := range value {
		switch {
		case char >= '0' && char <= '9':
		case char >= 'a' && char <= 'f':
		case char >= 'A' && char <= 'F':
		default:
			return false
		}
	}
	return true
}
