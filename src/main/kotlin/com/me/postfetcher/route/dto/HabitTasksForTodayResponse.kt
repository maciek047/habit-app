package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitTasksForTodayResponse (
    @Serializable
    val habits: List<HabitTaskDto>,
    val todayIndex: Int
)


@Serializable
class HabitTaskDto(
    val id: String,
    val day: Int,
    val habitName: String,
    val completed: Boolean
)
