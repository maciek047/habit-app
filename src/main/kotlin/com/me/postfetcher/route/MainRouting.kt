package com.me.postfetcher.route

import arrow.core.continuations.either
import com.me.postfetcher.AppError
import com.me.postfetcher.common.extensions.apiResponse
import com.me.postfetcher.common.extensions.toApiResponse
import com.me.postfetcher.database.model.createHabit
import com.me.postfetcher.database.model.deleteHabit
import com.me.postfetcher.database.model.editHabit
import com.me.postfetcher.database.model.editTodayHabitDay
import com.me.postfetcher.database.model.fetchHabitsWithPlannedDays
import com.me.postfetcher.database.model.fetchTodayHabits
import com.me.postfetcher.database.model.toWeeklyHabitDto
import com.me.postfetcher.route.dto.HabitCreateRequest
import com.me.postfetcher.route.dto.WeeklyHabitDto
import com.me.postfetcher.route.dto.HabitEditRequest
import com.me.postfetcher.route.dto.HabitTasksForTodayResponse
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
import java.time.LocalDate
import java.util.UUID

fun Route.mainRouting(
    postsFetcher: PostsFetcher
) {

    val logger = org.slf4j.LoggerFactory.getLogger("MainRouting")

    get("/habits") {
        val response =
            either<AppError, WeeklyHabitsResponse> {
               WeeklyHabitsResponse(fetchHabitsWithPlannedDays())
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    get("/habits/today") {
        val response =
            either<AppError, HabitTasksForTodayResponse> {
                HabitTasksForTodayResponse(fetchTodayHabits(), LocalDate.now().dayOfWeek.value - 1)
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    put("habits/today/{id}/complete/{completed}") {
        val response =
            either<AppError, HabitTasksForTodayResponse> {
                val id = call.parameters["id"] ?: throw Exception("Habit id is required")
                val completed = call.parameters["completed"] ?: throw Exception("Completed is required")
                logger.warn("id: $id, completed: $completed")
                val editedDay = editTodayHabitDay(UUID.fromString(id), completed.toBoolean())
                logger.warn("editedDay is completed: ${editedDay.completed}")
                HabitTasksForTodayResponse(fetchTodayHabits(), LocalDate.now().dayOfWeek.value - 1)
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
                editHabit(
                    id = id,
                    name = request.habitName,
                    days = request.days.map { it.dayOfWeek },
                    completedDays = request.days.filter { it.completed }.map { it.dayOfWeek }
                ).toWeeklyHabitDto()
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
