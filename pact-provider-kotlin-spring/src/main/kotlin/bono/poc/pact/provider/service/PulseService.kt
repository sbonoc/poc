package bono.poc.pact.provider.service

import bono.poc.pact.provider.model.Pulse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class PulseService {
    fun getPulse(from: LocalDate): Mono<Pulse> {
        return Mono.fromCallable {
            val now = Instant.now().toEpochMilli()
            val id = UUID.randomUUID().toString()
            // Logic to determine 'value' based on date, aligning with previous handler behavior
            Pulse(id, 1, from.toEpochDay(), now, 0)
        }
    }
}
