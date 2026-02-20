package com.agnostic.producer.routes

import com.agnostic.producer.domain.EventPublisher
import com.agnostic.producer.dto.PublishOrderRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry

fun Application.configureProducerRoutes(
    eventPublisher: EventPublisher,
    meterRegistry: MeterRegistry,
) {
    val requestCounter = meterRegistry.counter("orders.publish.requests")

    routing {
        post("/publish") {
            val request = call.receive<PublishOrderRequest>()
            request.validate()
            requestCounter.increment()
            log.debug("Received publish request orderId={} amount={}", request.id, request.amount)

            runCatching {
                eventPublisher.publish(request.toEvent())
                log.info("Accepted publish request orderId={}", request.id)
                call.respond(HttpStatusCode.Accepted, mapOf("status" to "accepted", "orderId" to request.id))
            }.onFailure { failure ->
                log.error("Failed to publish orderId={}", request.id, failure)
            }.getOrThrow()
        }
    }
}
