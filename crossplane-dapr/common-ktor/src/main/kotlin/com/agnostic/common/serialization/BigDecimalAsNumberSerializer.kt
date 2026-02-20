package com.agnostic.common.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

object BigDecimalAsNumberSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        val jsonEncoder = encoder as? JsonEncoder
        requireNotNull(jsonEncoder) { "BigDecimalAsNumberSerializer can only be used with JSON" }
        jsonEncoder.encodeJsonElement(JsonPrimitive(value))
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val jsonDecoder = decoder as? JsonDecoder
        requireNotNull(jsonDecoder) { "BigDecimalAsNumberSerializer can only be used with JSON" }
        val primitive = jsonDecoder.decodeJsonElement().jsonPrimitive
        return primitive.toBigDecimal()
    }

    private fun JsonPrimitive.toBigDecimal(): BigDecimal =
        runCatching { BigDecimal(this.content) }
            .getOrElse { error("Invalid decimal value: ${this.content}") }
}
