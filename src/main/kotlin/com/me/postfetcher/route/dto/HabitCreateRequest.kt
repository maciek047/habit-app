package com.me.postfetcher.route.dto

data class HabitCreateRequest(
    val habitName: String,
    val days: List<Int>
)