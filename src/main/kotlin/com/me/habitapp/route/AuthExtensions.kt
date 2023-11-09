package com.me.habitapp.route

import com.me.habitapp.database.model.User
import com.me.habitapp.database.model.createUser
import com.me.habitapp.database.model.findUserBySub
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun String.toUserAuthProfile(): UserAuthProfile {
    val json = Json.parseToJsonElement(this).jsonObject
    return UserAuthProfile(
        sub = json["sub"]!!.jsonPrimitive.content,
        email = json["email"]?.jsonPrimitive?.content,
        emailVerified = json["email_verified"]?.jsonPrimitive?.boolean ?: false,
        locale = json["locale"]?.jsonPrimitive?.content,
        name = json["name"]?.jsonPrimitive?.content,
        givenName = json["given_name"]?.jsonPrimitive?.content,
        familyName = json["family_name"]?.jsonPrimitive?.content,
        nickname = json["nickname"]?.jsonPrimitive?.content,
        picture = json["picture"]?.jsonPrimitive?.content
    )
}

suspend fun createUserFromAuthProfile(accessToken: String): User =
    try {
        createUser(getUserInfoFromToken(accessToken))
    } catch (e: Exception) {
        withContext(Dispatchers.IO) {
            Thread.sleep(200)
            findUserBySub(getUserInfoFromToken(accessToken).sub) ?: throw Exception("Failed to create user")
        }
    }

fun accessTokenFromCall(call: ApplicationCall): String {
    return call.request.header("Authorization")?.removePrefix("Bearer ")
        ?: throw Exception("No access token found")
}


suspend fun getUserInfoFromToken(token: String): UserAuthProfile {
    val domain = System.getenv("AUTH0_DOMAIN")
    val userInfoUrl = "https://$domain/userinfo"
    val httpClient = HttpClient(CIO)
    val userInfoResponse: String = httpClient.get(userInfoUrl) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
    }.body()
    return userInfoResponse.toUserAuthProfile()
}
