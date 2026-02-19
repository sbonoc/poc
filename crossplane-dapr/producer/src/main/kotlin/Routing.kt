package com.agnostic.producer

import io.dapr.client.DaprClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(dapr: DaprClient) {
    routing {
        post("/publish") {
            val order = call.receive<Order>()
            log.info("Publishing order $order")
            dapr.publishEvent("order-pubsub", "orders", order).block()
            call.respond(HttpStatusCode.Accepted)
        }
    }
}