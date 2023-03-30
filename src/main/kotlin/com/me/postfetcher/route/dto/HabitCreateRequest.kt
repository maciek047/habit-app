package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
data class HabitCreateRequest(
    val habitName: String,
    val days: List<Int>
)