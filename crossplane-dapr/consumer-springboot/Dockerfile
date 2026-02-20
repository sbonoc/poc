FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY common-ktor/build.gradle.kts common-ktor/build.gradle.kts
COPY producer-ktor/build.gradle.kts producer-ktor/build.gradle.kts
COPY consumer-ktor/build.gradle.kts consumer-ktor/build.gradle.kts
COPY producer-springboot/build.gradle.kts producer-springboot/build.gradle.kts
COPY consumer-springboot/build.gradle.kts consumer-springboot/build.gradle.kts

RUN chmod +x gradlew
RUN ./gradlew :consumer-springboot:dependencies --no-daemon

COPY consumer-springboot/src consumer-springboot/src
RUN ./gradlew :consumer-springboot:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /workspace/consumer-springboot/build/libs/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
