package com.agnostic.consumer.routes

import com.agnostic.common.events.OrderCreatedV1
import com.agnostic.common.serialization.JsonSupport
import com.agnostic.consumer.config.ConsumerSettings
import com.agnostic.consumer.domain.OrderEventHandler
import com.agnostic.consumer.infrastructure.OrderEventParser
import com.agnostic.consumer.plugins.configureHttpPlugins
import io.ktor.client.request.get
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

class ConsumerRoutesTest {
    @Test
    fun `subscribe endpoint returns dapr subscription`() =
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

            val response = client.get("/dapr/subscribe")

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains("order-pubsub")
            assertThat(response.bodyAsText()).contains("orders")
        }

    @Test
    fun `orders endpoint handles cloudevent payload`() =
        testApplication {
            val parser = OrderEventParser(JsonSupport.default)
            var received: OrderCreatedV1? = null
            val handler =
                OrderEventHandler { event ->
                    received = event
                }
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

            val response =
                client.post("/orders") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                          "id": "event-1",
                          "source": "producer",
                          "specversion": "1.0",
                          "type": "order.created.v1",
                          "data": {"id":"ORD-99","amount":99.9,"eventVersion":"v1"}
                        }
                        """.trimIndent(),
                    )
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(received?.id).isEqualTo("ORD-99")
        }

    @Test
    fun `configured subscription route is used for ingestion`() =
        testApplication {
            val parser = OrderEventParser(JsonSupport.default)
            var received: OrderCreatedV1? = null
            val handler = OrderEventHandler { event -> received = event }
            val subscription =
                ConsumerSettings.SubscriptionSettings(
                    pubSubName = "order-pubsub",
                    topicName = "orders",
                    route = "/orders-v2",
                )

            application {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                configureHttpPlugins(JsonSupport.default, registry)
                configureConsumerRoutes(parser, handler, subscription, registry)
            }

            val response =
                client.post("/orders-v2") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"id":"ORD-501","amount":88.1,"eventVersion":"v1"}""")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(received?.id).isEqualTo("ORD-501")
        }
}
