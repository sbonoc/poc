package com.agnostic.consumer.domain

import com.agnostic.common.events.OrderCreatedV1

fun interface OrderEventHandler {
    suspend fun handle(event: OrderCreatedV1)
}
