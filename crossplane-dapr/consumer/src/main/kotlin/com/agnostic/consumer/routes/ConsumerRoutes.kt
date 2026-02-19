package com.agnostic.consumer.routes

import com.agnostic.common.dapr.DaprSubscription
import com.agnostic.consumer.config.ConsumerSettings
import com.agnostic.consumer.domain.OrderEventHandler
import com.agnostic.consumer.infrastructure.OrderEventParser
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry

fun Application.configureConsumerRoutes(
    parser: OrderEventParser,
    eventHandler: OrderEventHandler,
    subscription: ConsumerSettings.SubscriptionSettings,
    meterRegistry: MeterRegistry,
) {
    val receivedCounter = meterRegistry.counter("orders.consume.requests")
    val eventRoute = subscription.route

    routing {
        get("/dapr/subscribe") {
            val subscriptions =
                listOf(
                    DaprSubscription(
                        pubsubname = subscription.pubSubName,
                        topic = subscription.topicName,
                        route = eventRoute,
                    ),
                )
            log.debug(
                "Serving Dapr subscription config pubsub={} topic={} route={}",
                subscription.pubSubName,
                subscription.topicName,
                eventRoute,
            )
            call.respond(subscriptions)
        }

        post(eventRoute) {
            receivedCounter.increment()
            val payload = call.receiveText()
            log.debug("Received event route={} payloadSize={}", eventRoute, payload.length)

            runCatching {
                val event = parser.parse(payload)
                eventHandler.handle(event)
                log.info("Consumed order event route={} id={} version={}", eventRoute, event.id, event.eventVersion)
                call.respond(HttpStatusCode.OK)
            }.onFailure { failure ->
                log.error("Failed to process event route={} payloadSize={}", eventRoute, payload.length, failure)
            }.getOrThrow()
        }
    }
}
