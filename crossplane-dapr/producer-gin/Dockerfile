FROM golang:1.23-alpine AS build
WORKDIR /src

COPY producer-gin/go.mod producer-gin/go.sum ./producer-gin/
WORKDIR /src/producer-gin
RUN go mod download

COPY producer-gin/. .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /out/producer-gin ./cmd/producer

FROM alpine:3.20
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /out/producer-gin /app/producer-gin
EXPOSE 8080
ENTRYPOINT ["/app/producer-gin"]
