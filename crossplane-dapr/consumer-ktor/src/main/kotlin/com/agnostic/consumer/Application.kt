package com.agnostic.consumer

import com.agnostic.common.observability.OpenTelemetryFactory
import com.agnostic.consumer.config.ConsumerSettings
import com.agnostic.consumer.domain.OrderEventHandler
import com.agnostic.consumer.infrastructure.LoggingOrderEventHandler
import com.agnostic.consumer.infrastructure.OrderEventParser
import com.agnostic.consumer.plugins.configureHttpPlugins
import com.agnostic.consumer.routes.configureConsumerRoutes
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
    val settings = ConsumerSettings.from(environment.config)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    val openTelemetry = OpenTelemetryFactory.create(settings.telemetry.serviceName)
    log.info(
        "Starting consumer serviceName={} pubsub={} topic={} route={}",
        settings.telemetry.serviceName,
        settings.subscription.pubSubName,
        settings.subscription.topicName,
        settings.subscription.route,
    )

    val eventHandler: OrderEventHandler =
        LoggingOrderEventHandler(
            logger = log,
            tracer = openTelemetry.getTracer("consumer-handler"),
            meterRegistry = meterRegistry,
        )

    val parser = OrderEventParser(settings.json)

    configureHttpPlugins(json = settings.json, meterRegistry = meterRegistry)
    configureConsumerRoutes(
        parser = parser,
        eventHandler = eventHandler,
        subscription = settings.subscription,
        meterRegistry = meterRegistry,
    )

    monitor.subscribe(ApplicationStopped) {
        log.info("Stopping consumer application")
        if (openTelemetry is OpenTelemetrySdk) {
            openTelemetry.sdkTracerProvider.close()
        }
        log.info("Consumer application stopped")
    }
}
