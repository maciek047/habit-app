package com.me.postfetcher.service

import arrow.core.Either
import arrow.core.continuations.either
import com.me.postfetcher.AppError
import com.me.postfetcher.client.Client
import com.me.postfetcher.domain.Comment
import com.me.postfetcher.domain.Post
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.time.Instant

const val GET_POSTS_BASE_URL = "https://jsonplaceholder.typicode.com"
const val GET_POSTS_SUFFIX = "posts"
const val GET_COMMENTS_SUFFIX = "comments"
const val POST_FILE_EXTENSION = ".json"

class PostsFetcher(private val client: Client, private val fileService: FileService) {

    suspend fun fetchPosts(): Either<AppError, String> = either {
        val posts: List<Post> = client.getObjectList<Post>("$GET_POSTS_BASE_URL/$GET_POSTS_SUFFIX").bind()
        val postsWithComments = runBlocking {
            posts.map {
                async {
                    it.fetchComments().bind()
                }
            }.map { it.await() }
        }

        fileService.saveObjectsToFiles(
            objects = postsWithComments,
            filePath = "posts-${Instant.now().toEpochMilli()}",
            fileExtension = POST_FILE_EXTENSION
        ).bind()
    }

    private suspend fun Post.fetchComments(): Either<AppError, Post> = either {
        val comments: List<Comment> =
            client.getObjectList<Comment>("$GET_POSTS_BASE_URL/$GET_COMMENTS_SUFFIX?postId=$id").bind()
        copy(comments = comments)
    }
}
