package com.me.postfetcher.route.dto

import java.util.UUID

class HabitDto (
    val id: UUID,
    val habitName: String,
    val days: List<Int>
)
