package com.agnostic.producer

import com.agnostic.common.observability.OpenTelemetryFactory
import com.agnostic.producer.config.ProducerSettings
import com.agnostic.producer.infrastructure.DaprOrderPublisher
import com.agnostic.producer.plugins.configureHttpPlugins
import com.agnostic.producer.routes.configureProducerRoutes
import io.dapr.client.DaprClientBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.EngineMain
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.sdk.OpenTelemetrySdk

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val settings = ProducerSettings.from(environment.config)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    val openTelemetry = OpenTelemetryFactory.create(settings.telemetry.serviceName)
    val daprClient = DaprClientBuilder().build()
    log.info(
        "Starting producer serviceName={} pubsub={} topic={}",
        settings.telemetry.serviceName,
        settings.dapr.pubSubName,
        settings.dapr.topicName,
    )

    val eventPublisher =
        DaprOrderPublisher(
            daprClient = daprClient,
            pubSubName = settings.dapr.pubSubName,
            topicName = settings.dapr.topicName,
            tracer = openTelemetry.getTracer("producer-publisher"),
            meterRegistry = meterRegistry,
        )

    configureHttpPlugins(json = settings.json, meterRegistry = meterRegistry)
    configureProducerRoutes(eventPublisher = eventPublisher, meterRegistry = meterRegistry)

    monitor.subscribe(ApplicationStopped) {
        log.info("Stopping producer application")
        daprClient.close()
        if (openTelemetry is OpenTelemetrySdk) {
            openTelemetry.sdkTracerProvider.close()
        }
        log.info("Producer application stopped")
    }
}
