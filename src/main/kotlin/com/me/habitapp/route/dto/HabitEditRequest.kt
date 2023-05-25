package com.me.habitapp.route.dto

import kotlinx.serialization.Serializable

@Serializable
data class HabitEditRequest(
    val habitName: String,
    val days: List<HabitDayDto>
)
