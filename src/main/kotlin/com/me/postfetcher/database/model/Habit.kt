package com.me.postfetcher.database.model

import com.me.postfetcher.database.getDateOfWeek
import com.me.postfetcher.database.splitToIntList
import com.me.postfetcher.route.dto.HabitDayDto
import com.me.postfetcher.route.dto.HabitTaskDto
import com.me.postfetcher.route.dto.WeeklyHabitDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object Habits : UUIDTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 255)
//    val days = varchar("days", 255)
//    val completedDays = varchar("completed_days", 255)
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

suspend fun fetchHabitsWithPlannedDays(): List<WeeklyHabitDto> {
    return fetchHabits().map { habit ->
        val plannedDays = fetchPlannedHabitDaysById(habit.id.value)
        WeeklyHabitDto(
            id = habit.id.value.toString(),
            habitName = habit.name,
            days = plannedDays.map {
                HabitDayDto(
                    dayOfWeek = it.day,
                    dateOfWeek = getDateOfWeek(it.day + 1).toString(),
                    completed = it.completed
                )
            }
        )
    }
}

suspend fun editHabit(id: String, name: String, days: List<Int>, completedDays: List<Int>, description: String = ""): Habit {
    newSuspendedTransaction {
        PlannedHabitDays.deleteWhere { PlannedHabitDays.habitId eq UUID.fromString(id) and (PlannedHabitDays.day notInList days) }
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
            this.days = days.joinToString(",")
        } ?: throw Exception("Habit not found")
    }
}

suspend fun deleteHabit(id: String): Habit {
    return newSuspendedTransaction {
        PlannedHabitDays.deleteWhere { PlannedHabitDays.habitId eq UUID.fromString(id) }
        Habit.findById(UUID.fromString(id))?.apply {
            this.delete()
        } ?: throw Exception("Habit not found. Nothing to delete.")
    }
}

suspend fun Habit.toWeeklyHabitDto(): WeeklyHabitDto {
    val habitDays = fetchPlannedHabitDaysById(this.id.value).map {
        HabitDayDto(
            dayOfWeek = it.day,
            dateOfWeek = getDateOfWeek(it.day + 1).toString(),
            completed = it.completed
        )
    }

    return WeeklyHabitDto(
        id = this.id.value.toString(),
        habitName = this.name,
        days = habitDays
    )
}
