package com.me.postfetcher.route

import arrow.core.continuations.either
import com.me.postfetcher.AppError
import com.me.postfetcher.AuthConfig
import com.me.postfetcher.common.extensions.apiResponse
import com.me.postfetcher.common.extensions.toApiResponse
import com.me.postfetcher.common.extensions.toLocalDate
import com.me.postfetcher.database.model.createHabit
import com.me.postfetcher.database.model.deleteHabit
import com.me.postfetcher.database.model.editHabit
import com.me.postfetcher.database.model.editTodayHabitDay
import com.me.postfetcher.database.model.fetchHabitMetrics
import com.me.postfetcher.database.model.fetchHabitStats
import com.me.postfetcher.database.model.fetchHabitsWithPlannedDays
import com.me.postfetcher.database.model.fetchTodayHabits
import com.me.postfetcher.database.model.toWeeklyHabitDto
import com.me.postfetcher.route.dto.HabitCreateRequest
import com.me.postfetcher.route.dto.HabitEditRequest
import com.me.postfetcher.route.dto.HabitMetricsResponse
import com.me.postfetcher.route.dto.HabitStatsRequest
import com.me.postfetcher.route.dto.HabitStatsResponse
import com.me.postfetcher.route.dto.HabitTasksForTodayResponse
import com.me.postfetcher.route.dto.WeeklyHabitDto
import com.me.postfetcher.route.dto.WeeklyHabitsResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.time.LocalDate
import java.util.UUID

fun Route.mainRouting(authConfig: AuthConfig) {

    val logger = mu.KotlinLogging.logger {}

    authenticate("auth0") {
        get("/habits") {
//            val user = call.attributes[UserKey]
//            println("user email: ${user.email}")
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            println("sub: $sub")
            val user = createUserFromAuthProfile(accessTokenFromCall(call))
            println("user email: ${user.email}")

            val response =
                either<AppError, WeeklyHabitsResponse> {
                    WeeklyHabitsResponse(fetchHabitsWithPlannedDays(user.id.value))
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    authenticate("auth0") {
        get("/habits/today") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            println("sub: $sub")
            val user = createUserFromAuthProfile(accessTokenFromCall(call))

            val response =
                either<AppError, HabitTasksForTodayResponse> {
                    HabitTasksForTodayResponse(fetchTodayHabits(), LocalDate.now().dayOfWeek.value - 1)
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    post("/habits") {
        val response =
            either<AppError, WeeklyHabitDto> {
                val request = call.receive<HabitCreateRequest>()
                createHabit(request.habitName, request.days).toWeeklyHabitDto()
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    post("/habits/stats") {
        val response =
            either<AppError, HabitStatsResponse> {
                val request = call.receive<HabitStatsRequest>()
                HabitStatsResponse(fetchHabitStats(request.startDate.toLocalDate(), request.endDate.toLocalDate()))
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

    get("/habits/completion-metrics") {
        val response =
            either<AppError, HabitMetricsResponse> {
                logger.info("fetching habit metrics")
                fetchHabitMetrics()
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
            either<AppError, Unit> {
                val id = call.parameters["id"] ?: throw Exception("Habit id is required")
                deleteHabit(id)
            }.toApiResponse(HttpStatusCode.OK)
        call.apiResponse(response)
    }

}
