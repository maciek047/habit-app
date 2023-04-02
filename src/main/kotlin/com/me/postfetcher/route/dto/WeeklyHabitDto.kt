package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyHabitDto (

    val id: String,
    val habitName: String,
    val days: List<HabitDayDto>
) {
    init {
        require(id.isNotBlank()) { "Id cannot be empty." }
        require(habitName.isNotEmpty()) { "Habit name cannot be empty." }
        require(days.isNotEmpty()) { "You must select at least one day." }
    }
}


