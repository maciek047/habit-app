package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitsResponse (
    @Serializable
    val habits: List<HabitDto>
)
