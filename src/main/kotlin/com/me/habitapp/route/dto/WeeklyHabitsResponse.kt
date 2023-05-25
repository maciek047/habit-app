package com.me.habitapp.route.dto

import kotlinx.serialization.Serializable

@Serializable
class WeeklyHabitsResponse (
    @Serializable
    val habits: List<WeeklyHabitDto>
)
