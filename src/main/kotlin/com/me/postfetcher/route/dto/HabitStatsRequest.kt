package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class HabitStatsRequest(
    val startDate: String,
    val endDate: String
)