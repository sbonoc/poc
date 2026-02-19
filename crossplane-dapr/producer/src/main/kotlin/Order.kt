package com.agnostic.producer

import kotlinx.serialization.Serializable

@Serializable
data class Order(val id: String, val amount: Double)