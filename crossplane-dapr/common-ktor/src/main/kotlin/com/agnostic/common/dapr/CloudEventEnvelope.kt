package com.agnostic.common.dapr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CloudEventEnvelope(
    val id: String? = null,
    val source: String? = null,
    val specversion: String? = null,
    val type: String? = null,
    val datacontenttype: String? = null,
    val data: JsonElement,
)
