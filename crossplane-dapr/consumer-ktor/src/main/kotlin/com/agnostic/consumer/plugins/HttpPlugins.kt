package com.agnostic.consumer.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.configureHttpPlugins(
    json: Json,
    meterRegistry: PrometheusMeterRegistry,
) {
    install(ContentNegotiation) {
        json(json)
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            call.request.path() !in setOf("/health/live", "/health/ready", "/metrics", "/dapr/config")
        }
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message.orEmpty()))
        }
        exception<SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid event payload"))
        }
        exception<Throwable> { call, cause ->
            meterRegistry.counter("http.server.errors").increment()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal server error"))
            call.application.environment.log.error("Unhandled error while processing request", cause)
        }
    }

    routing {
        get("/health/live") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        get("/health/ready") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        get("/metrics") {
            call.respondText(meterRegistry.scrape(), ContentType.parse("text/plain; version=0.0.4"))
        }
    }
}
