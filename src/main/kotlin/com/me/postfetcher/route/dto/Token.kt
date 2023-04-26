package com.me.postfetcher.route.dto

import kotlinx.serialization.Serializable

@Serializable
class Token(
    val access_token: String,
    val id_token: String,
    val scope: String,
    val expires_in: Int,
    val token_type: String
)