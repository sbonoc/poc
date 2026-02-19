package com.agnostic.consumer.infrastructure

import com.agnostic.common.dapr.CloudEventEnvelope
import com.agnostic.common.events.OrderCreatedV1
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.slf4j.LoggerFactory

class OrderEventParser(private val json: Json) {
    private companion object {
        private val logger = LoggerFactory.getLogger(OrderEventParser::class.java)
    }

    fun parse(rawPayload: String): OrderCreatedV1 {
        val cloudEventResult =
            runCatching {
                val cloudEvent = json.decodeFromString<CloudEventEnvelope>(rawPayload)
                json.decodeFromJsonElement(OrderCreatedV1.serializer(), cloudEvent.data)
            }

        if (cloudEventResult.isSuccess) {
            val event = cloudEventResult.getOrThrow()
            logger.debug("Parsed payload as CloudEvent id={} version={}", event.id, event.eventVersion)
            return event
        }

        val cloudEventError = cloudEventResult.exceptionOrNull()
        logger.warn(
            "Payload was not a valid CloudEvent, attempting raw OrderCreatedV1 parse cloudEventError={}",
            cloudEventError?.message,
        )
        return runCatching {
            json.decodeFromString<OrderCreatedV1>(rawPayload)
        }.onSuccess { event ->
            logger.debug("Parsed payload as raw event id={} version={}", event.id, event.eventVersion)
        }.getOrElse { rawError ->
            logger.error(
                "Failed to parse payload as CloudEvent and raw OrderCreatedV1 cloudEventError={} rawError={}",
                cloudEventError?.message,
                rawError.message,
            )
            throw IllegalArgumentException("payload is neither a CloudEvent nor an OrderCreatedV1 event", rawError)
        }
    }
}
