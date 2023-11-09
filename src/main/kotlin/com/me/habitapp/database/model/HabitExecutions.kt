package com.me.habitapp.database.model

import com.me.habitapp.database.getDateOfWeek
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
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
        val user = reference("habitexecution_user_id", Users)
        val executionDate = date("completion_date")
        val completed = bool("completed").default(false)
    }

    class HabitExecution(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<HabitExecution>(HabitExecutions)

        var habitId by HabitExecutions.habitId
        var plannedHabitDayId by HabitExecutions.plannedHabitDayId
        var executionDate by HabitExecutions.executionDate
        var completed by HabitExecutions.completed
        var user by HabitExecutions.user
    }


suspend fun ensureHabitExecutionsForCurrentWeekExist(userId: UUID) {
    val startDate = getDateOfWeek(1)
    val endDate = getDateOfWeek(7)

    if (getHabitExecutionsByDateRange(userId, startDate, endDate).isEmpty()) {
        newSuspendedTransaction {
            val plannedDays = PlannedHabitDay.find { PlannedHabitDays.user eq userId }.toList()
            HabitExecutions.batchInsert(plannedDays) { plannedDay ->
                this[HabitExecutions.habitId] = plannedDay.habitId
                this[HabitExecutions.plannedHabitDayId] = plannedDay.id
                this[HabitExecutions.executionDate] = getDateOfWeek(plannedDay.day + 1)
                this[HabitExecutions.completed] = false
                this[HabitExecutions.user] = EntityID(userId, Users)
            }
        }
    }
}


suspend fun getHabitExecutionsByDateRange(
    userId: UUID,
    startDate: LocalDate,
    endDate: LocalDate
): List<HabitExecution> {
    return newSuspendedTransaction {
        HabitExecution.find {
            (HabitExecutions.user eq userId) and
                    (HabitExecutions.executionDate greaterEq startDate) and
                    (HabitExecutions.executionDate lessEq endDate)
        }.toList()
    }
}

fun createHabitExecution(
    userId: UUID,
    habitId: UUID,
    plannedHabitDayId: UUID,
    dayOfWeek: Int,
    completed: Boolean = false
): HabitExecution {
    val dateOfExecution = getDateOfWeek(dayOfWeek + 1)
    return HabitExecution.new {
        this.habitId = EntityID(habitId, Habits)
        this.plannedHabitDayId = EntityID(plannedHabitDayId, PlannedHabitDays)
        this.executionDate = dateOfExecution
        this.completed = completed
        this.user = EntityID(userId, Users)
    }

}

fun createHabitExecutionsBatch(habitExecutionsData: List<HabitExecutionData>) {
        HabitExecutions.batchInsert(habitExecutionsData) { data ->
            this[HabitExecutions.habitId] = EntityID(data.habitId, Habits)
            this[HabitExecutions.plannedHabitDayId] = EntityID(data.plannedHabitDayId, PlannedHabitDays)
            this[HabitExecutions.executionDate] = getDateOfWeek(data.dayOfWeek + 1)
            this[HabitExecutions.completed] = data.completed
            this[HabitExecutions.user] = EntityID(data.userId, Users)
        }
}

data class HabitExecutionData(
    val userId: UUID,
    val habitId: UUID,
    val plannedHabitDayId: UUID,
    val dayOfWeek: Int,
    val completed: Boolean = false
)
