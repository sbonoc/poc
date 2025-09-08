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

@Component
class PulseHandler(private val pulseService: PulseService) {

    fun getPulses(request: ServerRequest): Mono<ServerResponse> {
        val from: LocalDate = when (val fromParam = request.queryParam("from").orElse("today")) {
            "today" -> LocalDate.now()
            else -> try {
                LocalDate.parse(fromParam)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("Invalid 'from' parameter. Must be 'today' or a valid date (YYYY-MM-DD).")
            }
        }

        return pulseService.getPulse(from)
            .flatMap { pulse ->
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(pulse)
            }
            .onErrorResume(IllegalArgumentException::class.java) { e ->
                ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("error" to e.message))
            }
    }
}
