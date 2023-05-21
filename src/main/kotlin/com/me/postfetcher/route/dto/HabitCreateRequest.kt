package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
data class HabitCreateRequest(
    val habitName: String,
    val days: List<Int>
)

fun HabitCreateRequest.isValid(): Boolean = habitName.isNotEmpty() && days.isNotEmpty()
