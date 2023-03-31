package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class WeeklyHabitsResponse (
    @Serializable
    val habits: List<WeeklyHabitDto>
)
