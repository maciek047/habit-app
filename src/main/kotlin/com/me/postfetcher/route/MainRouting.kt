package com.me.postfetcher.route

import arrow.core.continuations.either
import com.me.postfetcher.AppError
import com.me.postfetcher.common.extensions.apiResponse
import com.me.postfetcher.common.extensions.toApiResponse
import com.me.postfetcher.database.createHabit
import com.me.postfetcher.database.fetchHabits
import com.me.postfetcher.database.toDto
import com.me.postfetcher.route.dto.HabitsResponse
import com.me.postfetcher.service.PostsFetcher
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get


private val logger = mu.KotlinLogging.logger {}


fun Route.mainRouting(
    postsFetcher: PostsFetcher
) {
    get("/habits") {
        val response =
            either<AppError, HabitsResponse> {
                logger.info("Fetching habits")
                val habits = fetchHabits()
                logger.info("Fetched habits: $habits")
                HabitsResponse(fetchHabits().map { it.toDto() })
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    get("/test") {
        val response =
            either<AppError, PostsResponse> {
               PostsResponse("It works!")
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }


}
