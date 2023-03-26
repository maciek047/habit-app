package com.me.postfetcher.route.dto

data class HabitEditRequest(
    val id: String,
    val habitName: String,
    val days: List<Int>
)
