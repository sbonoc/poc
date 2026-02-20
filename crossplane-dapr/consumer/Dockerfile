FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY common/build.gradle.kts common/build.gradle.kts
COPY producer/build.gradle.kts producer/build.gradle.kts
COPY consumer/build.gradle.kts consumer/build.gradle.kts

RUN chmod +x gradlew
RUN ./gradlew :consumer:dependencies --no-daemon

COPY common/src common/src
COPY consumer/src consumer/src

RUN ./gradlew :consumer:buildFatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/consumer/build/libs/consumer-all.jar app.jar

ENTRYPOINT ["java", "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
