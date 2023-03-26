package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitDto (
    val id: String,
    val habitName: String,
    val days: List<Int>
)
