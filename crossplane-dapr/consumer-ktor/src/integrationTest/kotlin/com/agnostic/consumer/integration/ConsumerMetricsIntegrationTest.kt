package com.agnostic.consumer.integration

import com.agnostic.common.serialization.JsonSupport
import com.agnostic.consumer.config.ConsumerSettings
import com.agnostic.consumer.domain.OrderEventHandler
import com.agnostic.consumer.infrastructure.OrderEventParser
import com.agnostic.consumer.plugins.configureHttpPlugins
import com.agnostic.consumer.routes.configureConsumerRoutes
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConsumerMetricsIntegrationTest {
    @Test
    fun `metrics endpoint exposes consume counter`() =
        testApplication {
            val parser = OrderEventParser(JsonSupport.default)
            val handler = OrderEventHandler { _ -> }
            val subscription =
                ConsumerSettings.SubscriptionSettings(
                    pubSubName = "order-pubsub",
                    topicName = "orders",
                    route = "/orders",
                )

            application {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                configureHttpPlugins(JsonSupport.default, registry)
                configureConsumerRoutes(parser, handler, subscription, registry)
            }

            client.post("/orders") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"ORD-123","amount":41.0,"eventVersion":"v1"}""")
            }

            val metricsPayload = client.get("/metrics").bodyAsText()

            assertThat(metricsPayload).contains("orders_consume_requests_total")
        }
}
