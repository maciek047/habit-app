package com.me.habitapp.database.model

import com.me.habitapp.database.formatDate
import com.me.habitapp.database.getDateOfWeek
import com.me.habitapp.database.model.HabitExecutions.executionDate
import com.me.habitapp.database.model.HabitExecutions.user
import com.me.habitapp.route.dto.HabitDayDto
import com.me.habitapp.route.dto.HabitMetricsDto
import com.me.habitapp.route.dto.HabitMetricsResponse
import com.me.habitapp.route.dto.HabitStatsDto
import com.me.habitapp.route.dto.HabitTaskDto
import com.me.habitapp.route.dto.WeeklyHabitDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object Habits : UUIDTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val user = reference("user_id", Users)
}

class Habit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Habit>(Habits)

    var name by Habits.name
    var description by Habits.description
    var createdAt by Habits.createdAt
    var user by Habits.user
}

suspend fun createFirstHabit(userId: UUID): Habit {
    return newSuspendedTransaction {
        val days = listOf(0, 1, 2, 3, 4, 5, 6)
        val name = "My first habit"
        val description = "This is my first habit"

        createHabit(userId, name, days, description)
    }
}

suspend fun createHabit(userId: UUID, name: String, days: List<Int>, description: String = ""): Habit {
    return newSuspendedTransaction {
        val createdHabit = Habit.new {
            this.name = name
            this.description = description
            this.createdAt = LocalDateTime.now()
            this.user = EntityID(userId, Users)
        }

        PlannedHabitDays.batchInsert(days) { day ->
            this[PlannedHabitDays.habitId] = createdHabit.id
            this[PlannedHabitDays.day] = day
            this[PlannedHabitDays.completed] = false
            this[PlannedHabitDays.user] = EntityID(userId, Users)
        }

        createdHabit
    }
}

suspend fun fetchTodayHabits(userId: UUID): List<HabitTaskDto> {
    val today = LocalDate.now().dayOfWeek.value - 1
    return fetchHabitTasksForGivenDay(userId, today)
}

suspend fun fetchHabitTasksForGivenDay(userId: UUID, day: Int): List<HabitTaskDto> {
    return newSuspendedTransaction {
        Habits.innerJoin(PlannedHabitDays, onColumn = { Habits.id }, otherColumn = { habitId })
            .select {
                (Habits.user eq userId) and
                        (PlannedHabitDays.day eq day)
            }
            .orderBy(Habits.createdAt)
            .map { row ->
                val id = row[Habits.id].toString()
                val habitName = row[Habits.name]
                val completed = row[PlannedHabitDays.completed]
                HabitTaskDto(id, day, habitName, completed)
            }
    }
}

suspend fun fetchHabitMetrics(userId: UUID): HabitMetricsResponse {
    ensureHabitExecutionsForCurrentWeekExist(userId)
    val startDate = LocalDate.now().minusMonths(3)
    val endDate = LocalDate.now()

    val totalCountAlias = executionDate.count().alias("total_count")
    val completedCountAlias = Sum(
        Case().When(HabitExecutions.completed eq true, intLiteral(1)).Else(intLiteral(0)),
        IntegerColumnType()
    ).alias("completed_count")

    val query = HabitExecutions
        .slice(
            executionDate,
            totalCountAlias,
            completedCountAlias
        )
        .select {
            (user eq userId) and
                    (executionDate.between(startDate, endDate))
        }
        .groupBy(executionDate)
        .orderBy(executionDate)

    val daysList = newSuspendedTransaction {
        query.map { row ->
            val date = row[executionDate].toString()
            val completedCount = row[completedCountAlias] ?: 0
            val totalCount = row[totalCountAlias]
            HabitMetricsDto(date, completedCount, totalCount.toInt())
        }
    }

    return HabitMetricsResponse(startDate.toString(), endDate.toString(), daysList)
}



suspend fun fetchHabitStats(userId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitStatsDto> {
    val completedCountAlias = Sum(
        Case()
            .When(HabitExecutions.completed eq true, intLiteral(1))
            .Else(intLiteral(0)),
        IntegerColumnType()
    ).alias("completed_count")

    val totalCountAlias = executionDate.count().alias("total_count")

    return newSuspendedTransaction {
        HabitExecutions.join(Habits, JoinType.INNER, onColumn = HabitExecutions.habitId, otherColumn = Habits.id)
            .slice(
                Habits.id,
                Habits.name,
                completedCountAlias,
                totalCountAlias
            )
            .select {
                (user eq userId) and
                        (executionDate.between(startDate, endDate))
            }
            .groupBy(Habits.id, Habits.name)
            .map { row ->
                val habitName = row[Habits.name]
                val completed: Int = row[completedCountAlias] ?: 0
                val totalCount: Long = row[totalCountAlias]
                HabitStatsDto(habitName, completed, totalCount.toInt())
            }
    }
}

suspend fun fetchHabitsWithPlannedDays(userId: UUID): List<WeeklyHabitDto> {
    val userHabits = newSuspendedTransaction {
        Habit.find { Habits.user eq userId }.toList()
    }

    val habitIds: List<UUID> = userHabits.map { it.id.value }

    val allPlannedDays = newSuspendedTransaction {
        PlannedHabitDay.find { PlannedHabitDays.habitId inList habitIds }.toList()
    }

    val plannedDaysByHabitId: Map<UUID, List<PlannedHabitDay>> = allPlannedDays.groupBy { it.habitId.value }

    return userHabits.map { habit ->
        val plannedDays = plannedDaysByHabitId[habit.id.value].orEmpty().map { plannedDay ->
            HabitDayDto(
                dayOfWeek = plannedDay.day,
                dateOfWeek = formatDate(getDateOfWeek(plannedDay.day + 1)),
                completed = plannedDay.completed,
                isBeforeCreationDate = getDateOfWeek(plannedDay.day + 1).isBefore(habit.createdAt.toLocalDate())
            )
        }

        WeeklyHabitDto(
            id = habit.id.value.toString(),
            habitName = habit.name,
            days = plannedDays
        )
    }
}


val DAYS_OF_WEEK = listOf(0,1,2,3,4,5,6)

suspend fun editHabit(userId: UUID, id: String, name: String, days: List<Int>, completedDays: List<Int>): Habit {
    return newSuspendedTransaction {
        val habitId = UUID.fromString(id)
        val today = LocalDate.now().dayOfWeek.value - 1

        // Delete HabitExecutions for days no longer in the plan and not completed
        val habitDaysToDelete = DAYS_OF_WEEK.filter { !days.contains(it) }
            .map { getDateOfWeek(it + 1) }
            .filter { it.isAfter(getDateOfWeek(today).minusDays(1)) }

        HabitExecutions.deleteWhere {
            (HabitExecutions.habitId eq habitId) and
                    (executionDate inList habitDaysToDelete) and
                    (completed eq false)
        }

        // Delete PlannedHabitDays for days no longer in the plan
        PlannedHabitDays.deleteWhere {
            (PlannedHabitDays.habitId eq habitId) and
                    (day notInList days)
        }

        // Fetch existing planned days
        val existingPlannedDays = PlannedHabitDay.find {
            PlannedHabitDays.habitId eq habitId
        }.map { it.day }

        val newDays = days.subtract(existingPlannedDays.toSet())
        if (newDays.isNotEmpty()) {
            createPlannedHabitDaysBatch(userId, habitId, newDays)
        }

        val plannedDaysToUpdate = PlannedHabitDay.find {
            (PlannedHabitDays.habitId eq habitId) and
                    (PlannedHabitDays.day inList days.intersect(existingPlannedDays).toList())
        }.toList()

        plannedDaysToUpdate.forEach { plannedDay ->
            plannedDay.completed = completedDays.contains(plannedDay.day)
        }

        val habit = Habit.findById(habitId)?.apply {
            this.name = name
            this.description = description
        } ?: throw Exception("Habit not found")

        habit
    }
}


suspend fun deleteHabit(id: String) {
    return newSuspendedTransaction {

        val today = LocalDate.now().dayOfWeek.value - 1
        val habitDaysToDelete = DAYS_OF_WEEK
            .map { getDateOfWeek(it + 1) }
            .filter { it.isAfter(getDateOfWeek(today).minusDays(1)) }
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
