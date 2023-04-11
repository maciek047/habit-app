package com.me.postfetcher.database.model

import com.me.postfetcher.database.getDateOfWeek
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

object PlannedHabitDays : UUIDTable() {
    val habitId = reference("habit_id", Habits)
    val day = integer("day")
    val completed = bool("completed").default(false)
}

val logger = org.slf4j.LoggerFactory.getLogger("MainRouting")


class PlannedHabitDay(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PlannedHabitDay>(PlannedHabitDays)

    var habitId by PlannedHabitDays.habitId
    var day by PlannedHabitDays.day
    var completed by PlannedHabitDays.completed
}

suspend fun editPlannedHabitDay(habitId: UUID, day: Int, completed: Boolean): PlannedHabitDay {
    return newSuspendedTransaction {
        val plannedHabitDay = PlannedHabitDay.find { (PlannedHabitDays.habitId eq habitId) and (PlannedHabitDays.day eq day) }.first()
        plannedHabitDay.completed = completed

        val habitDate = getDateOfWeek(day + 1)
        val habitExecution = HabitExecution.find {
            (HabitExecutions.plannedHabitDayId eq plannedHabitDay.id.value) and HabitExecutions.executionDate.eq(habitDate)
        }.first()
        habitExecution.completed = completed

        plannedHabitDay
    }
}

suspend fun editTodayHabitDay(habitId: UUID, completed: Boolean): PlannedHabitDay {
    val today = LocalDateTime.now().dayOfWeek.value - 1
    logger.warn("Today is $today")
    return editPlannedHabitDay(habitId, today, completed)
}

suspend fun createPlannedHabitDay(habitId: UUID, day: Int): PlannedHabitDay {
    return newSuspendedTransaction {
        val plannedDay = PlannedHabitDay.new {
            this.habitId = EntityID(habitId, Habits)
            this.day = day
            this.completed = false
        }

        val dateOfExecution = getDateOfWeek(day + 1)
        if(!dateOfExecution.isBefore(LocalDateTime.now().toLocalDate())) {
            createHabitExecution(habitId, plannedDay.id.value, day)
        }

        plannedDay
    }
}

suspend fun fetchPlannedHabitDaysById(habitId: UUID): List<PlannedHabitDay> {
    return newSuspendedTransaction {
        PlannedHabitDay.find { PlannedHabitDays.habitId eq habitId }.toList()
    }
}
