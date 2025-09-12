# Stage 1: Build the application
FROM golang:1.25 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy go.mod and go.sum first to leverage Docker's cache
# This step is only re-run if go.mod or go.sum changes
COPY go.mod .
COPY go.sum .

# Download Go modules
RUN go mod download

# Copy the rest of the application source code
COPY . .

# Build the application
# CGO_ENABLED=0 is used to create a statically linked binary, which is good for scratch/alpine images
# -o app specifies the output binary name
RUN CGO_ENABLED=0 go build -o app .

# Install Pact Go CLI
RUN CGO_ENABLED=1 go install github.com/pact-foundation/pact-go/v2@latest
RUN PATH=$PATH:$(go env GOPATH)/bin pact-go install

# Run tests (optional, but highly recommended for CI/CD)
# This step will fail the build if any tests fail
RUN go test -v .

# Stage 2: Create the final lightweight image
FROM alpine:latest AS backend

# Set the working directory
WORKDIR /app

# Copy the compiled binary from the builder stage
COPY --from=builder /app/app .

# Expose the port the application listens on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["./app"]
