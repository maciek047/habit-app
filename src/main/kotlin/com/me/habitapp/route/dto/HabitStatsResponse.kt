package com.me.habitapp.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitStatsResponse(
    val habits: List<HabitStatsDto>
)

@Serializable
class HabitStatsDto(
    val habitName: String,
    val score: Int,
    val maxScore: Int
)