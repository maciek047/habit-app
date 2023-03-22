package com.me.postfetcher.common.dependency

import com.me.postfetcher.client.Client
import com.me.postfetcher.service.FileService
import com.me.postfetcher.service.PostsFetcher

fun dependencies(): Dependencies {
    val client = Client()
    val fileService = FileService()
    val postsFetcher = PostsFetcher(client, fileService)

    return Dependencies(postsFetcher, fileService, client)
}

data class Dependencies(
    val postsFetcher: PostsFetcher,
    val fileService: FileService,
    val client: Client
)
