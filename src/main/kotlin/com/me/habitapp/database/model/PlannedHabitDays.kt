package com.me.habitapp.database.model

import com.me.habitapp.database.getDateOfWeek
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

object PlannedHabitDays : UUIDTable() {
    val habitId = reference("habit_id", Habits)
    val day = integer("day")
    val completed = bool("completed").default(false)
    val user = reference(name = "plannedhabitday_user_id", foreign = Users)
}

val logger = org.slf4j.LoggerFactory.getLogger("MainRouting")


class PlannedHabitDay(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PlannedHabitDay>(PlannedHabitDays)

    var habitId by PlannedHabitDays.habitId
    var day by PlannedHabitDays.day
    var completed by PlannedHabitDays.completed
    var user by PlannedHabitDays.user

}

fun editPlannedHabitDay(habitId: UUID, day: Int, completed: Boolean): PlannedHabitDay {
        val plannedHabitDay = PlannedHabitDay.find { (PlannedHabitDays.habitId eq habitId) and (PlannedHabitDays.day eq day) }.first()
        plannedHabitDay.completed = completed

        val habitDate = getDateOfWeek(day + 1)
    HabitExecution.find {
        (HabitExecutions.plannedHabitDayId eq plannedHabitDay.id.value) and HabitExecutions.executionDate.eq(habitDate)
    }.firstOrNull()?.let { it.completed = completed } ?: createHabitExecution(
        plannedHabitDay.user.value,
        habitId,
        plannedHabitDay.id.value,
        day,
        completed
    )
        return plannedHabitDay
}

suspend fun editTodayHabitDay(habitId: UUID, completed: Boolean): PlannedHabitDay {
    val today = LocalDateTime.now().dayOfWeek.value - 1
    logger.warn("Today is $today")
    return newSuspendedTransaction {
        editPlannedHabitDay(habitId, today, completed)
    }
}

fun createPlannedHabitDay(userId: UUID, habitId: UUID, day: Int): PlannedHabitDay {
    val plannedDay = PlannedHabitDay.new {
        this.habitId = EntityID(habitId, Habits)
        this.day = day
        this.completed = false
        this.user = EntityID(userId, Users)
    }

    val dateOfExecution = getDateOfWeek(day + 1)
    if (!dateOfExecution.isBefore(LocalDateTime.now().toLocalDate())) {
        createHabitExecution(
            userId = userId,
            habitId = habitId,
            plannedHabitDayId = plannedDay.id.value,
            dayOfWeek = day
        )
    }

        return plannedDay
}

fun createPlannedHabitDaysBatch(userId: UUID, habitId: UUID, days: Collection<Int>) {
    // Create and insert planned days in batch
    val insertedPlannedDays = PlannedHabitDays.batchInsert(days) { day ->
        this[PlannedHabitDays.habitId] = EntityID(habitId, Habits)
        this[PlannedHabitDays.day] = day
        this[PlannedHabitDays.completed] = false
        this[PlannedHabitDays.user] = EntityID(userId, Users)
    }

    // Prepare data for habit executions
    val habitExecutionsData = insertedPlannedDays.mapNotNull { insertedDay ->
        val dayOfWeek = insertedDay[PlannedHabitDays.day]
        val dateOfExecution = getDateOfWeek(dayOfWeek + 1)
        if (!dateOfExecution.isBefore(LocalDateTime.now().toLocalDate())) {
            HabitExecutionData(
                userId = userId,
                habitId = habitId,
                plannedHabitDayId = insertedDay[PlannedHabitDays.id].value,
                dayOfWeek = dayOfWeek,
                completed = false
            )
        } else null
    }

    // Create habit executions in batch if needed
    if (habitExecutionsData.isNotEmpty()) {
        createHabitExecutionsBatch(habitExecutionsData)
    }

}

suspend fun fetchPlannedHabitDaysById(habitId: UUID): List<PlannedHabitDay> {
    return newSuspendedTransaction {
        PlannedHabitDay.find {
            (PlannedHabitDays.habitId eq habitId)
        }.toList()
    }
}
