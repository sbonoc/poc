package com.agnostic.consumer

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

fun Application.configureRouting() {
    routing {
        get("/dapr/subscribe") {
            // This tells the Dapr sidecar: "Take messages from the 'orders-bus' 
            // topic on the 'order-pubsub' component and POST them to my '/orders' route."
            val subscription = """
            [
            {
                "pubsubname": "order-pubsub",
                "topic": "orders",
                "route": "/orders"
            }
            ]
            """.trimIndent()
            
            call.respondText(subscription, io.ktor.http.ContentType.Application.Json)
        }
        post("/orders") {
            val rawPayload = call.receiveText()
            try {
                // Parse the CloudEvent wrapper manually
                val cloudEvent = Json.parseToJsonElement(rawPayload).jsonObject
                val data = cloudEvent["data"]
                log.info("🚀 RECEIVED BY CONSUMER: $data")
                // Acknowledge receipt to Dapr
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                log.error("Failed to parse event", e)
                call.respond(HttpStatusCode.InternalServerError)
            }

        }
    }
}