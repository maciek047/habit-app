package com.me.postfetcher.client

import arrow.core.Either
import arrow.core.continuations.either
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.me.postfetcher.AppError.ClientError
import com.me.postfetcher.common.extensions.isJsonArray
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.runBlocking

class Client {

    suspend inline fun <reified T : Any> getObjectList(getUrl: String): Either<ClientError, List<T>> =
        either {
            runBlocking {
                val client = HttpClient(CIO)

                val responseJsonString = client.prepareGet(getUrl).execute().toResponseString()

                val gson = Gson()
                when (responseJsonString.isJsonArray()) {
                    true -> {
                        val type = object : TypeToken<List<T>>() {}.type
                        val receivedObjectList: List<T> = gson.fromJson(responseJsonString, type)
                        receivedObjectList
                    }
                    else -> {
                        val type = object : TypeToken<T>() {}.type
                        val receivedObjectList: List<T> = listOf(gson.fromJson(responseJsonString, type))
                        receivedObjectList
                    }
                }
            }
        }
}

suspend fun HttpResponse.toResponseString(): String {
    val channel: ByteReadChannel = body()
    var response: ByteArray = byteArrayOf()
    while (!channel.isClosedForRead) {
        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
        while (!packet.isEmpty) {
            val bytes = packet.readBytes()
            response += bytes
            channel.availableForRead
        }
    }
    return String(response)
}
