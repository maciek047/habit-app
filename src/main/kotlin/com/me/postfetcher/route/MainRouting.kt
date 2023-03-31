package com.me.postfetcher.route

import arrow.core.continuations.either
import com.me.postfetcher.AppError
import com.me.postfetcher.common.extensions.apiResponse
import com.me.postfetcher.common.extensions.toApiResponse
import com.me.postfetcher.database.createHabit
import com.me.postfetcher.database.deleteHabit
import com.me.postfetcher.database.editHabit
import com.me.postfetcher.database.fetchHabits
import com.me.postfetcher.database.fetchTodayHabits
import com.me.postfetcher.database.toHabitForTodayDto
import com.me.postfetcher.database.toWeeklyHabitDto
import com.me.postfetcher.route.dto.HabitCreateRequest
import com.me.postfetcher.route.dto.WeeklyHabitDto
import com.me.postfetcher.route.dto.HabitEditRequest
import com.me.postfetcher.route.dto.HabitsForTodayResponse
import com.me.postfetcher.route.dto.WeeklyHabitsResponse
import com.me.postfetcher.service.PostsFetcher
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put


fun Route.mainRouting(
    postsFetcher: PostsFetcher
) {

    get("/habits") {
        val response =
            either<AppError, WeeklyHabitsResponse> {
               WeeklyHabitsResponse(fetchHabits().map { it.toWeeklyHabitDto() })
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    get("/habits/today") {
        val response =
            either<AppError, HabitsForTodayResponse> {
                HabitsForTodayResponse(fetchTodayHabits().map { it.toHabitForTodayDto() })
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    post("/habits") {
        val response =
            either<AppError, WeeklyHabitDto> {
                val request = call.receive<HabitCreateRequest>()
                createHabit(request.habitName, request.days).toWeeklyHabitDto()
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    put("/habits/{id}") {
        val response =
            either<AppError, WeeklyHabitDto> {
                val id = call.parameters["id"] ?: throw Exception("Habit id is required")
                val request = call.receive<HabitEditRequest>()

                editHabit(id, request.habitName, request.days.map { it.dayOfWeek }, request.days.filter { it.completed }.map { it.dayOfWeek}).toWeeklyHabitDto()
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    delete("/habits/{id}") {
        val response =
            either<AppError, WeeklyHabitDto> {
                val id = call.parameters["id"] ?: throw Exception("Habit id is required")
                deleteHabit(id).toWeeklyHabitDto()
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }



}
