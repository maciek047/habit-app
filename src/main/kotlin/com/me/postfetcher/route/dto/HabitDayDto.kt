package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitDayDto (
    val dayOfWeek: Int,
    val completed: Boolean
)