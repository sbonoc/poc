package bono.poc

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PulseService {
    fun getPulse(from: LocalDate): Pulse {
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        return Pulse(id, 1, from.toEpochDay(), now, 0)
    }

}
