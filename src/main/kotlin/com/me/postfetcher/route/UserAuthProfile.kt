package com.me.postfetcher.route

data class UserAuthProfile(
    val sub: String,
    val email: String?,
    val emailVerified: Boolean,
    val locale: String?,
    val name: String?,
    val givenName: String?,
    val familyName: String?,
    val nickname: String?,
    val picture: String?
)