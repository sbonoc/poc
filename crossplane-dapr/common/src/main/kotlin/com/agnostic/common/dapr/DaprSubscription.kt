package com.agnostic.common.dapr

import kotlinx.serialization.Serializable

@Serializable
data class DaprSubscription(
    val pubsubname: String,
    val topic: String,
    val route: String,
)
