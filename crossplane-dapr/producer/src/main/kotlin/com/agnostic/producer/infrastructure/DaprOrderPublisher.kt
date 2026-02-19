package com.agnostic.producer.infrastructure

import com.agnostic.common.events.OrderCreatedV1
import com.agnostic.producer.domain.EventPublisher
import io.dapr.client.DaprClient
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory

class DaprOrderPublisher(
    private val daprClient: DaprClient,
    private val pubSubName: String,
    private val topicName: String,
    private val tracer: Tracer,
    meterRegistry: MeterRegistry,
) : EventPublisher {
    private companion object {
        private val logger = LoggerFactory.getLogger(DaprOrderPublisher::class.java)
    }

    private val publishedCounter = meterRegistry.counter("orders.published")

    override suspend fun publish(order: OrderCreatedV1) {
        val span =
            tracer.spanBuilder("orders.publish")
                .setSpanKind(SpanKind.PRODUCER)
                .startSpan()

        try {
            logger.debug("Publishing order event id={} pubsub={} topic={}", order.id, pubSubName, topicName)
            runCatching {
                daprClient.publishEvent(pubSubName, topicName, order).awaitSingleOrNull()
            }.onFailure { failure ->
                logger.error(
                    "Failed publishing order event id={} pubsub={} topic={}",
                    order.id,
                    pubSubName,
                    topicName,
                    failure,
                )
                span.recordException(failure)
                span.setStatus(StatusCode.ERROR)
                throw failure
            }
            logger.info("Published order event id={} version={}", order.id, order.eventVersion)
            publishedCounter.increment()
            span.setStatus(StatusCode.OK)
        } finally {
            span.end()
        }
    }
}
