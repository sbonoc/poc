package com.agnostic.producer.config

import com.agnostic.common.events.EventTopics
import com.agnostic.common.serialization.JsonSupport
import io.ktor.server.config.ApplicationConfig
import kotlinx.serialization.json.Json

data class ProducerSettings(
    val dapr: DaprSettings,
    val telemetry: TelemetrySettings,
    val json: Json,
) {
    data class DaprSettings(
        val pubSubName: String,
        val topicName: String,
    )

    data class TelemetrySettings(
        val serviceName: String,
    )

    companion object {
        fun from(config: ApplicationConfig): ProducerSettings {
            val pubSubName =
                System.getenv("DAPR_PUBSUB_NAME")
                    ?: config.propertyOrNull("producer.dapr.pubSubName")?.getString()
                    ?: EventTopics.PUBSUB_COMPONENT
            val topicName =
                System.getenv("DAPR_TOPIC_NAME")
                    ?: config.propertyOrNull("producer.dapr.topicName")?.getString()
                    ?: EventTopics.ORDERS_TOPIC
            val serviceName =
                System.getenv("OTEL_SERVICE_NAME")
                    ?: config.propertyOrNull("producer.telemetry.serviceName")?.getString()
                    ?: "producer"

            return ProducerSettings(
                dapr = DaprSettings(pubSubName = pubSubName, topicName = topicName),
                telemetry = TelemetrySettings(serviceName = serviceName),
                json = JsonSupport.default,
            )
        }
    }
}
