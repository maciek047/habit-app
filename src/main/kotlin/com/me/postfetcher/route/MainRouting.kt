package com.me.postfetcher.route

import arrow.core.continuations.either
import com.me.postfetcher.AppError
import com.me.postfetcher.common.extensions.apiResponse
import com.me.postfetcher.common.extensions.toApiResponse
import com.me.postfetcher.database.createHabit
import com.me.postfetcher.service.PostsFetcher
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.mainRouting(
    postsFetcher: PostsFetcher
) {
    get("/posts") {
        val response =
            either<AppError, PostsResponse> {
                PostsResponse(postsFetcher.fetchPosts().bind())
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    get("/test") {
        val response =
            either<AppError, PostsResponse> {
                createHabit("very first habit", "habit description")
                PostsResponse("It works!")
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }
}
