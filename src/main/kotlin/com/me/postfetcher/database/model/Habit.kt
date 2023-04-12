package com.me.postfetcher.database.model

import com.me.postfetcher.common.extensions.getByPropertyName
import com.me.postfetcher.database.formatDate
import com.me.postfetcher.database.getDateOfWeek
import com.me.postfetcher.database.model.HabitExecutions.executionDate
import com.me.postfetcher.route.dto.HabitDayDto
import com.me.postfetcher.route.dto.HabitMetricsDto
import com.me.postfetcher.route.dto.HabitMetricsResponse
import com.me.postfetcher.route.dto.HabitStatsDto
import com.me.postfetcher.route.dto.HabitTaskDto
import com.me.postfetcher.route.dto.WeeklyHabitDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object Habits : UUIDTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

class Habit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Habit>(Habits)

    var name by Habits.name
    var description by Habits.description
    var createdAt by Habits.createdAt
}

suspend fun createHabit(name: String, days: List<Int>, description: String = ""): Habit {
    val createdHabit = newSuspendedTransaction {
        Habit.new {
            this.name = name
            this.description = description
            this.createdAt = LocalDateTime.now()
        }
    }
    days.forEach { day ->
        createPlannedHabitDay(createdHabit.id.value, day)
    }
    return createdHabit
}

suspend fun fetchHabits(): List<Habit> {
    return newSuspendedTransaction {
        Habit.all().sortedBy { Habits.createdAt }.toList()
    }
}

suspend fun fetchTodayHabits(): List<HabitTaskDto> {
    val today = LocalDate.now().dayOfWeek.value - 1
    return fetchHabitTasksForGivenDay(today)
}

suspend fun fetchHabitTasksForGivenDay(day: Int): List<HabitTaskDto> {
    return newSuspendedTransaction {
        Habits.innerJoin(PlannedHabitDays, onColumn = { Habits.id }, otherColumn = { habitId })
            .select { PlannedHabitDays.day eq day }
            .orderBy(Habits.createdAt)
            .map { row ->
                val id = row[Habits.id].toString()
                val habitName = row[Habits.name]
                val completed = row[PlannedHabitDays.completed]
                HabitTaskDto(id, day, habitName, completed)
            }
    }
}

suspend fun fetchHabitMetrics(): HabitMetricsResponse {
    ensureHabitExecutionsForCurrentWeekExist()
    val startDate = LocalDate.now().minusMonths(3)
    val endDate = LocalDate.now()

    val completedCountAlias = Sum(
        Case()
            .When(HabitExecutions.completed eq true, intLiteral(1))
            .Else(intLiteral(0)),
        IntegerColumnType()
    ).alias("completed_count")

    val totalCountAlias = executionDate.count().alias("total_count")

    val daysList =  newSuspendedTransaction {
        HabitExecutions
            .slice(
                executionDate,
                completedCountAlias,
                totalCountAlias
            )
            .select { executionDate.between(startDate, endDate) }
            .groupBy(executionDate)
            .map { row ->
                val date = row[executionDate]
                val completed: Int = row[completedCountAlias] ?: 0
                val totalCount: Long = row[totalCountAlias]
                HabitMetricsDto(date.toString(), completed, totalCount.toInt())
            }
    }
    return HabitMetricsResponse(startDate.toString(), endDate.toString(), daysList)
}

suspend fun fetchHabitStats(startDate: LocalDate, endDate: LocalDate): List<HabitStatsDto> {
    val completedCountAlias = Sum(
        Case()
            .When(HabitExecutions.completed eq true, intLiteral(1))
            .Else(intLiteral(0)),
        IntegerColumnType()
    ).alias("completed_count")

    val totalCountAlias = executionDate.count().alias("total_count")

    return  newSuspendedTransaction {
        HabitExecutions.join(Habits, JoinType.INNER, onColumn = HabitExecutions.habitId, otherColumn = Habits.id)
            .slice(
                Habits.id,
                Habits.name,
                completedCountAlias,
                totalCountAlias
            )
            .select { executionDate.between(startDate, endDate) }
            .groupBy(Habits.id, Habits.name)
            .map { row ->
                val habitName = row[Habits.name]
                val completed: Int = row[completedCountAlias] ?: 0
                val totalCount: Long = row[totalCountAlias]
                HabitStatsDto(habitName, completed, totalCount.toInt())
            }
    }
}

suspend fun fetchHabitsWithPlannedDays(): List<WeeklyHabitDto> {
    ensureHabitExecutionsForCurrentWeekExist()
    return fetchHabits().map { habit ->
        val plannedDays = fetchPlannedHabitDaysById(habit.id.value)
        WeeklyHabitDto(
            id = habit.id.value.toString(),
            habitName = habit.name,
            days = plannedDays.map {
                HabitDayDto(
                    dayOfWeek = it.day,
                    dateOfWeek = formatDate(getDateOfWeek(it.day + 1)),
                    completed = it.completed,
                    isBeforeCreationDate = getDateOfWeek(it.day + 1).isBefore(habit.createdAt.toLocalDate())
                )
            }
        )
    }
}

val DAYS_OF_WEEK = listOf(0,1,2,3,4,5,6)

suspend fun editHabit(id: String, name: String, days: List<Int>, completedDays: List<Int>): Habit {
    newSuspendedTransaction {
        val today = LocalDate.now().dayOfWeek.value - 1
        val habitDaysToDelete = DAYS_OF_WEEK.filter { !days.contains(it) }
            .map { getDateOfWeek(it + 1) }
            .filter { it.isAfter(getDateOfWeek(today).minus(12, ChronoUnit.HOURS)) }
        HabitExecutions.deleteWhere {
            habitId eq UUID.fromString(id) and
                    (executionDate inList habitDaysToDelete) and
                    (completed eq false) //do not delete execution if it was completed
        }

        PlannedHabitDays.deleteWhere { habitId eq UUID.fromString(id) and (day notInList days) }
        val plannedDays = fetchPlannedHabitDaysById(UUID.fromString(id)).map { it.day }
        days.forEach { day ->
            if (!plannedDays.contains(day)) {
                createPlannedHabitDay(UUID.fromString(id), day)
            } else {
                editPlannedHabitDay(UUID.fromString(id), day, completedDays.contains(day))
            }
        }
    }
    return newSuspendedTransaction {
        Habit.findById(UUID.fromString(id))?.apply {
            this.name = name
            this.description = description
        } ?: throw Exception("Habit not found")
    }
}

suspend fun deleteHabit(id: String): Habit {
    return newSuspendedTransaction {

        val today = LocalDate.now().dayOfWeek.value - 1
        val habitDaysToDelete = DAYS_OF_WEEK
            .map { getDateOfWeek(it + 1) }
            .filter { it.isAfter(getDateOfWeek(today).minus(12, ChronoUnit.HOURS)) }
        HabitExecutions.deleteWhere {
            habitId eq UUID.fromString(id) and
                    (executionDate inList habitDaysToDelete) and
                    (completed eq false) //do not delete execution if it was completed
        }

        PlannedHabitDays.deleteWhere { habitId eq UUID.fromString(id) }
        Habit.findById(UUID.fromString(id))?.apply {
            this.delete()
        } ?: throw Exception("Habit not found. Nothing to delete.")
    }
}

suspend fun Habit.toWeeklyHabitDto(): WeeklyHabitDto {
    val habitDays = fetchPlannedHabitDaysById(this.id.value).map {
        HabitDayDto(
            dayOfWeek = it.day,
            dateOfWeek = formatDate(getDateOfWeek(it.day + 1)),
            completed = it.completed,
            isBeforeCreationDate = getDateOfWeek(it.day + 1).isBefore(createdAt.toLocalDate())
        )
    }
    return WeeklyHabitDto(
        id = this.id.value.toString(),
        habitName = this.name,
        days = habitDays
    )
}
