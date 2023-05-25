package com.me.habitapp.common.dependency

import com.me.habitapp.client.Client

fun dependencies(): Dependencies {
    val client = Client()

    return Dependencies(client)
}

data class Dependencies(
    val client: Client
)
