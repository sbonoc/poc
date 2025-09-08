package bono.poc.pact.provider.config

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(-1) // Ensure this filter runs early in the chain
class LoggingWebFilter : WebFilter {

    private val logger = LoggerFactory.getLogger(LoggingWebFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        // Log request details
        logger.info("Incoming Request: {} {} from {}", request.method, request.uri, request.remoteAddress)
        request.headers.forEach { (name, values) ->
            logger.debug("  Request Header - {}: {}", name, values.joinToString(","))
        }

        return chain.filter(exchange)
            .doFinally { _ ->
                // Log response details after the chain has completed
                logger.info("Outgoing Response: {} {} with status {}", request.method, request.uri, response.statusCode)
                response.headers.forEach { (name, values) ->
                    logger.debug("  Response Header - {}: {}", name, values.joinToString(","))
                }
            }
    }
}
