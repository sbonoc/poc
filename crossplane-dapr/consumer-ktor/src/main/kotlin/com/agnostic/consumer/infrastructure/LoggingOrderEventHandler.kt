package com.agnostic.consumer.infrastructure

import com.agnostic.common.events.OrderCreatedV1
import com.agnostic.consumer.domain.OrderEventHandler
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import org.slf4j.Logger

class LoggingOrderEventHandler(
    private val logger: Logger,
    private val tracer: Tracer,
    meterRegistry: MeterRegistry,
) : OrderEventHandler {
    private val processedCounter = meterRegistry.counter("orders.consumed")

    override suspend fun handle(event: OrderCreatedV1) {
        val span =
            tracer.spanBuilder("orders.consume")
                .setSpanKind(SpanKind.CONSUMER)
                .startSpan()

        try {
            runCatching {
                logger.debug("Handling order event id={} amount={}", event.id, event.amount)
                logger.info(
                    "Received order event id={} amount={} version={}",
                    event.id,
                    event.amount,
                    event.eventVersion,
                )
                processedCounter.increment()
            }.onFailure { failure ->
                logger.error("Failed to handle order event id={}", event.id, failure)
                span.recordException(failure)
                span.setStatus(StatusCode.ERROR)
            }.getOrThrow()
            span.setStatus(StatusCode.OK)
        } finally {
            span.end()
        }
    }
}
