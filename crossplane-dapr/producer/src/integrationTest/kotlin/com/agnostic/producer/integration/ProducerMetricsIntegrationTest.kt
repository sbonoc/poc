package com.agnostic.producer.integration

import com.agnostic.common.serialization.JsonSupport
import com.agnostic.producer.domain.EventPublisher
import com.agnostic.producer.plugins.configureHttpPlugins
import com.agnostic.producer.routes.configureProducerRoutes
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

class ProducerMetricsIntegrationTest {
    @Test
    fun `metrics endpoint exposes publish counter`() =
        testApplication {
            val fakePublisher = EventPublisher { _ -> }

            application {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                configureHttpPlugins(JsonSupport.default, registry)
                configureProducerRoutes(fakePublisher, registry)
            }

            client.post("/publish") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"ORD-200","amount":15.0}""")
            }

            val metricsPayload = client.get("/metrics").bodyAsText()

            assertThat(metricsPayload).contains("orders_publish_requests_total")
        }
}
