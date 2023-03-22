package com.me.postfetcher.domain

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
    val comments: List<Comment>? = null
)
