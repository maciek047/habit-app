package com.me.habitapp.route

import arrow.core.continuations.either
import com.me.habitapp.AppError
import com.me.habitapp.AppError.ValidationError
import com.me.habitapp.common.extensions.ApiResponse.ErrorResponse
import com.me.habitapp.common.extensions.apiResponse
import com.me.habitapp.common.extensions.toApiResponse
import com.me.habitapp.common.extensions.toLocalDate
import com.me.habitapp.database.model.*
import com.me.habitapp.route.dto.HabitCreateRequest
import com.me.habitapp.route.dto.HabitEditRequest
import com.me.habitapp.route.dto.HabitMetricsResponse
import com.me.habitapp.route.dto.HabitStatsRequest
import com.me.habitapp.route.dto.HabitStatsResponse
import com.me.habitapp.route.dto.HabitTasksForTodayResponse
import com.me.habitapp.route.dto.WeeklyHabitDto
import com.me.habitapp.route.dto.WeeklyHabitsResponse
import com.me.habitapp.route.dto.isValid
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

fun Route.mainRouting() {

    val logger = mu.KotlinLogging.logger {}
    val userCreationMutex = Mutex()

    authenticate("auth0") {
        get("/habits") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }

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
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }
            val response =
                either<AppError, HabitTasksForTodayResponse> {
                    HabitTasksForTodayResponse(fetchTodayHabits(user.id.value), LocalDate.now().dayOfWeek.value - 1)
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    authenticate("auth0") {
        post("/habits") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }
            val response =
                either<AppError, WeeklyHabitDto> {
                    val request = call.receive<HabitCreateRequest>()
                    if (!request.isValid()) {
                        call.apiResponse(
                            ErrorResponse(
                                HttpStatusCode.BadRequest,
                                ValidationError("Habit name and days must be populated")
                            )
                        )
                    }
                    createHabit(user.id.value, request.habitName, request.days).toWeeklyHabitDto()
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    authenticate("auth0") {
        post("/habits/stats") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }
            val response =
                either<AppError, HabitStatsResponse> {
                    val request = call.receive<HabitStatsRequest>()
                    HabitStatsResponse(
                        fetchHabitStats(
                            userId = user.id.value,
                            startDate = request.startDate.toLocalDate(),
                            endDate = request.endDate.toLocalDate()
                        )
                    )
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    authenticate("auth0") {
        get("/habits/completion-metrics") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }

            val response =
                either<AppError, HabitMetricsResponse> {
                    logger.info("fetching habit metrics")
                    fetchHabitMetrics(user.id.value)
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    authenticate("auth0") {
        put("habits/today/{id}/complete/{completed}") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }

            val response =
                either<AppError, HabitTasksForTodayResponse> {
                    val id = call.parameters["id"] ?: throw Exception("Habit id is required")
                    val completed = call.parameters["completed"] ?: throw Exception("Completed is required")
                    logger.warn("id: $id, completed: $completed")
                    val editedDay = editTodayHabitDay(UUID.fromString(id), completed.toBoolean())
                    logger.warn("editedDay is completed: ${editedDay.completed}")
                    HabitTasksForTodayResponse(fetchTodayHabits(user.id.value), LocalDate.now().dayOfWeek.value - 1)
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    authenticate("auth0") {
        put("/habits/{id}") {
            val principal = call.authentication.principal<JWTPrincipal>()
            val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")
            val user = UserCache.getUser(sub) ?: userCreationMutex.withLock {
                UserCache.getUser(sub) ?: findUserBySub(sub)?.also { UserCache.addUser(sub, it) }
                ?: createUserFromAuthProfile(accessTokenFromCall(call)).also { UserCache.addUser(sub, it) }
            }

            val response =
                either<AppError, WeeklyHabitDto> {
                    val id = call.parameters["id"] ?: throw Exception("Habit id is required")
                    val request = call.receive<HabitEditRequest>()
                    editHabit(
                        userId = user.id.value,
                        id = id,
                        name = request.habitName,
                        days = request.days.map { it.dayOfWeek },
                        completedDays = request.days.filter { it.completed }.map { it.dayOfWeek }
                    ).toWeeklyHabitDto()
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
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

object UserCache {
    private data class CachedUser(val user: User, val timestamp: Long)

    private val cache = ConcurrentHashMap<String, CachedUser>()
    private val cacheDuration = TimeUnit.MINUTES.toMillis(30)

    fun getUser(sub: String): User? {
        val cachedUser = cache[sub] ?: return null
        if (System.currentTimeMillis() - cachedUser.timestamp > cacheDuration) {
            cache.remove(sub)
            return null
        }
        return cachedUser.user
    }

    fun addUser(sub: String, user: User) {
        cache[sub] = CachedUser(user, System.currentTimeMillis())
    }

    fun updateUser(sub: String, user: User) {
        cache[sub] = CachedUser(user, System.currentTimeMillis())
    }

    fun invalidateUser(sub: String) {
        cache.remove(sub)
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { now - it.value.timestamp > cacheDuration }
    }
}