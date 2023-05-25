package com.me.habitapp.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitStatsRequest(
    val startDate: String,
    val endDate: String
)