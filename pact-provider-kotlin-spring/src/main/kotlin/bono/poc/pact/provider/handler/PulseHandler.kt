package bono.poc.pact.provider.handler

import bono.poc.pact.provider.model.Pulse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Component
class PulseHandler {

    fun getPulses(request: ServerRequest): Mono<ServerResponse> {
        val fromParam = request.queryParam("from").orElse("today")

        val pulseValue = when (fromParam) {
            "today" -> 1 // Example: return 1 for "today"
            else -> {
                try {
                    // Attempt to parse as a date, if successful, return a different value
                    LocalDate.parse(fromParam)
                    2 // Example: return 2 for a valid date
                } catch (e: DateTimeParseException) {
                    // If not "today" and not a valid date, return a default or error
                    return ServerResponse.badRequest()
                        .bodyValue(mapOf("error" to "Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD)."))
                }
            }
        }

        val pulse = Pulse(pulseValue)
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(pulse)
    }
}
