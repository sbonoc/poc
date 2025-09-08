package bono.poc

import kotlinx.serialization.Serializable

@Serializable
data class Pulse(
    val id: String,
    val value: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long
)
