package bono.poc.pact.provider.router

import bono.poc.pact.provider.handler.PulseHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

@Configuration
class PulseRouter(private val pulseHandler: PulseHandler) {

    @Bean
    fun pulseRoutes() = router {
        GET("/api/pulses", pulseHandler::getPulses)
    }
}
