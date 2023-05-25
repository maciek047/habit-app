package com.me.habitapp.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitMetricsResponse (
    val startDate: String,
    val endDate: String,
    val metrics: List<HabitMetricsDto>
)

@Serializable
class HabitMetricsDto (
    val date: String,
    val score: Int,
    val maxScore: Int
)