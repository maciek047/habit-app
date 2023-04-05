package com.me.postfetcher.database.model

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.util.UUID

object HabitExecutions : UUIDTable() {
    val plannedHabitDayId = reference("planned_habit_day_id", PlannedHabitDays)
    val executionDate = datetime("completion_date")
    val completed = bool("completed").default(false)
}


class HabitExecution(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<HabitExecution>(HabitExecutions)

    var plannedHabitDayId by HabitExecutions.plannedHabitDayId
    var executionDate by HabitExecutions.executionDate
    var completed by HabitExecutions.completed
}
