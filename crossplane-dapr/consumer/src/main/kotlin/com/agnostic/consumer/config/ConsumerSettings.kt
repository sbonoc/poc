package com.agnostic.consumer.config

import com.agnostic.common.events.EventTopics
import com.agnostic.common.serialization.JsonSupport
import io.ktor.server.config.ApplicationConfig
import kotlinx.serialization.json.Json

data class ConsumerSettings(
    val subscription: SubscriptionSettings,
    val telemetry: TelemetrySettings,
    val json: Json,
) {
    data class SubscriptionSettings(
        val pubSubName: String,
        val topicName: String,
        val route: String,
    )

    data class TelemetrySettings(
        val serviceName: String,
    )

    companion object {
        fun from(config: ApplicationConfig): ConsumerSettings {
            val pubSubName =
                System.getenv("DAPR_PUBSUB_NAME")
                    ?: config.propertyOrNull("consumer.subscription.pubSubName")?.getString()
                    ?: EventTopics.PUBSUB_COMPONENT
            val topicName =
                System.getenv("DAPR_TOPIC_NAME")
                    ?: config.propertyOrNull("consumer.subscription.topicName")?.getString()
                    ?: EventTopics.ORDERS_TOPIC
            val route =
                (
                    System.getenv("DAPR_SUBSCRIPTION_ROUTE")
                        ?: config.propertyOrNull("consumer.subscription.route")?.getString()
                        ?: "/orders"
                ).let { configuredRoute ->
                    if (configuredRoute.startsWith("/")) configuredRoute else "/$configuredRoute"
                }
            val serviceName =
                System.getenv("OTEL_SERVICE_NAME")
                    ?: config.propertyOrNull("consumer.telemetry.serviceName")?.getString()
                    ?: "consumer"

            return ConsumerSettings(
                subscription = SubscriptionSettings(pubSubName = pubSubName, topicName = topicName, route = route),
                telemetry = TelemetrySettings(serviceName = serviceName),
                json = JsonSupport.default,
            )
        }
    }
}
