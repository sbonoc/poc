package bono.poc.pact.provider.handler

import bono.poc.pact.provider.model.Pulse
import bono.poc.pact.provider.service.PulseService
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.springframework.http.HttpStatus

@Component
class PulseHandler(private val pulseService: PulseService) {

    fun getPulses(request: ServerRequest): Mono<ServerResponse> {
        // First, create a Mono that handles the parsing of the 'from' parameter.
        // This ensures that any synchronous exceptions during parsing are wrapped in a Mono.error().
        val dateMono: Mono<LocalDate> = Mono.defer {
            val fromParam = request.queryParam("from").orElse("today")
            when (fromParam) {
                "today" -> Mono.just(LocalDate.now())
                else -> try {
                    Mono.just(LocalDate.parse(fromParam))
                } catch (e: DateTimeParseException) {
                    // Emit IllegalArgumentException as an error in the Mono stream
                    Mono.error(IllegalArgumentException("Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD)."))
                }
            }
        }

        // Now, chain the call to pulseService.getPulse and error handling
        return dateMono
            .flatMap { date -> pulseService.getPulse(date) } // Call the service with the parsed date
            .flatMap { pulse ->
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(pulse)
            }
            .onErrorResume(IllegalArgumentException::class.java) { e ->
                // This now correctly catches IllegalArgumentException emitted by the dateMono
                ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("error" to e.message))
            }
            .onErrorResume(Exception::class.java) { e ->
                // This catches any other exceptions (e.g., from pulseService)
                ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("error" to ("Internal Server Error: ${e.message}")))
            }
    }
}
