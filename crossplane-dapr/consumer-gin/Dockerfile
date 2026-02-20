FROM golang:1.23-alpine AS build
WORKDIR /src

COPY consumer-gin/go.mod ./consumer-gin/go.mod
WORKDIR /src/consumer-gin
RUN go mod download

COPY consumer-gin/. .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /out/consumer-gin ./cmd/consumer

FROM alpine:3.20
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /out/consumer-gin /app/consumer-gin
EXPOSE 8080
ENTRYPOINT ["/app/consumer-gin"]
