package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitsForTodayResponse (
    @Serializable
    val habits: List<HabitForTodayDto>
)

@Serializable
class HabitForTodayDto (
    val id: String,
    val name: String,
    val completed: Boolean
)
