package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitsResponse (
    val habits: List<HabitDto>
)
