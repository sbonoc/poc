package com.agnostic.common.serialization

import kotlinx.serialization.json.Json

object JsonSupport {
    val default: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }
}
