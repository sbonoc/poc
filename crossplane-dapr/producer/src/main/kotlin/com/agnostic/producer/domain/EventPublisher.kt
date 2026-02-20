package com.agnostic.producer.domain

import com.agnostic.common.events.OrderCreatedV1

fun interface EventPublisher {
    suspend fun publish(order: OrderCreatedV1)
}
