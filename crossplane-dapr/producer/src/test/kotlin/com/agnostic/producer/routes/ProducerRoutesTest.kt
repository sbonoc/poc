package com.agnostic.producer.routes

import com.agnostic.common.events.OrderCreatedV1
import com.agnostic.common.serialization.JsonSupport
import com.agnostic.producer.domain.EventPublisher
import com.agnostic.producer.plugins.configureHttpPlugins
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProducerRoutesTest {
    @Test
    fun `publish endpoint accepts valid order`() =
        testApplication {
            var publishedEvent: OrderCreatedV1? = null
            val fakePublisher =
                EventPublisher { event ->
                    publishedEvent = event
                }

            application {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                configureHttpPlugins(JsonSupport.default, registry)
                configureProducerRoutes(fakePublisher, registry)
            }

            val response =
                client.post("/publish") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"id":"ORD-100","amount":123.45}""")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.Accepted)
            assertThat(response.bodyAsText()).contains("accepted")
            assertThat(publishedEvent).isNotNull
            assertThat(publishedEvent?.id).isEqualTo("ORD-100")
        }

    @Test
    fun `publish endpoint rejects invalid payload`() =
        testApplication {
            val fakePublisher = EventPublisher { _ -> }

            application {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                configureHttpPlugins(JsonSupport.default, registry)
                configureProducerRoutes(fakePublisher, registry)
            }

            val response =
                client.post("/publish") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"id":"ORD-100","amount":0}""")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
            assertThat(response.bodyAsText()).contains("amount")
        }
}
