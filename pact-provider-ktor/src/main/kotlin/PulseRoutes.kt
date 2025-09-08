package bono.poc

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Application.configureRouting(pulseService: PulseService) {

    routing {
        route("/api/pulses") {

            get {
                val fromParam = call.request.queryParameters["from"]
                log.info("Received GET request for pulses from $fromParam")
                var fromDate: LocalDate = LocalDate.now()
                val isValid = when (fromParam) {
                    null, "today" -> true
                    else -> {
                        // Try to parse as a date (e.g., YYYY-MM-DD)
                        try {
                            fromDate = LocalDate.parse(fromParam)
                            true
                        } catch (e: DateTimeParseException) {
                            false // Not a valid date format
                        }
                    }
                }
                if (isValid) {
                    val result = pulseService.getPulse(fromDate)
                    log.info("Returning pulse: $result")
                    call.respond(HttpStatusCode.OK, result)

                } else {
                    log.info("Invalid 'from' parameter: $fromParam")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD).")
                    )
                }

            }

        }
    }
}
