# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

# Copy Gradle wrapper files first to leverage Docker cache.
# If only source code changes, these layers won't be rebuilt.
COPY gradlew .
COPY gradlew.bat .
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY gradle/wrapper gradle/wrapper/

# Copy build configuration files.
# This layer will be cached as long as build.gradle.kts or settings.gradle.kts don't change.
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies. This step will be cached if build.gradle.kts and settings.gradle.kts don't change.
# Running a dummy task like 'dependencies' or 'resolveDependencies' ensures dependencies are downloaded.
# Using --offline for subsequent builds if dependencies are already cached.
RUN ./gradlew dependencies --no-daemon

# Copy source code. This layer will be rebuilt only if source code changes.
COPY src src/

# Build the application
# Use --no-daemon for CI/CD environments to avoid lingering processes
# Use -x test to skip tests during build if not needed for the image
RUN ./gradlew build --no-daemon

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre-alpine AS backend

# Create a non-root user and group for security best practices
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

WORKDIR /app

# Copy the built application from the builder stage
# The 'build' task places the runnable distribution in build/install/<project-name>
# Ensure the copied files are owned by the non-root user
COPY --from=builder --chown=appuser:appgroup /app/build/install/pact-provider-kotlin-spring ./

# Expose the port Spring runs on (default is 8080)
EXPOSE 8080

# Run the application
# The 'build' task creates a shell script in the 'bin' directory
CMD ["./bin/pact-provider-kotlin-spring"]
