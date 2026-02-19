package com.agnostic.producer

import io.dapr.client.DaprClientBuilder
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Configure features
    configureSerialization()

    // Configure routing, passing the DaprClient as a dependency
    configureRouting(dapr = DaprClientBuilder().build())
}