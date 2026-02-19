# Stage 1: Build using the project's own Gradle Wrapper
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /home/gradle/src

# Copy the gradle wrapper and configuration first (for layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Give execution permission to the wrapper
RUN chmod +x gradlew

# Download dependencies (this layer is cached unless build.gradle.kts changes)
RUN ./gradlew --version

# Copy source and build
COPY src src
RUN ./gradlew buildFatJar --no-daemon

# Stage 2: Tiny Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for SRE security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built jar from the build stage
COPY --from=build /home/gradle/src/build/libs/consumer-all.jar .

# Use Optimized JVM flags for container environments
ENTRYPOINT ["java", "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=75.0", "-jar", "consumer-all.jar"]