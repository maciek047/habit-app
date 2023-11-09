package com.me.habitapp.route.dto

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyHabitDto (
    val id: String,
    val habitName: String,
    val days: List<HabitDayDto>
) {
    init {
        require(id.isNotBlank()) { "Id cannot be empty." }
    }
}
