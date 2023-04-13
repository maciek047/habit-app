package com.me.postfetcher.database.model

import com.me.postfetcher.database.getDateOfWeek
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

    object HabitExecutions : UUIDTable() {
        val habitId = reference(
            name = "habit_id",
            foreign = Habits,
            onDelete = ReferenceOption.SET_NULL
        ).nullable()
        val plannedHabitDayId = reference(
            name = "planned_habit_day_id",
            foreign = PlannedHabitDays,
            onDelete = ReferenceOption.SET_NULL
        ).nullable()
        val executionDate = date("completion_date")
        val completed = bool("completed").default(false)
    }

    class HabitExecution(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<HabitExecution>(HabitExecutions)

        var habitId by HabitExecutions.habitId
        var plannedHabitDayId by HabitExecutions.plannedHabitDayId
        var executionDate by HabitExecutions.executionDate
        var completed by HabitExecutions.completed
    }

suspend fun ensureHabitExecutionsForCurrentWeekExist() {
    val startDate = getDateOfWeek(1)
    val endDate = getDateOfWeek(7)

    if(getHabitExecutionsByDateRange(startDate, endDate).isEmpty()) {
        newSuspendedTransaction {
            PlannedHabitDay.all().forEach {
                createHabitExecution(it.habitId.value, it.id.value, it.day, it.completed)
            }
        }
    }
}

suspend fun getHabitExecutionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<HabitExecution> {
    return newSuspendedTransaction {
        HabitExecution.find {
                    (HabitExecutions.executionDate greaterEq startDate) and
                    (HabitExecutions.executionDate lessEq endDate)
        }.toList()
    }
}

fun createHabitExecution(habitId: UUID, plannedHabitDayId: UUID, dayOfWeek: Int, completed: Boolean = false): HabitExecution {
    val dateOfExecution = getDateOfWeek(dayOfWeek + 1)
        return HabitExecution.new {
            this.habitId = EntityID(habitId, Habits)
            this.plannedHabitDayId = EntityID(plannedHabitDayId, PlannedHabitDays)
            this.executionDate = dateOfExecution
            this.completed = completed
        }

}
