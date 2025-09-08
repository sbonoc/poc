package bono.poc

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun Application.configureSerialization() {
    install(ServerContentNegotiation) {
        json()
    }
}
