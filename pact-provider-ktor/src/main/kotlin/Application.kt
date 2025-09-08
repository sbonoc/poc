package bono.poc

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Configure features
    configureSerialization()

    // Configure routing, passing the service as a dependency
    configureRouting(pulseService = PulseService())
}
