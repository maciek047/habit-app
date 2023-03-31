package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class WeeklyHabitDto (
    val id: String,
    val habitName: String,
    val days: List<HabitDayDto>
)
