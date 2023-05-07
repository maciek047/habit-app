package com.me.postfetcher.route

import com.me.postfetcher.database.model.User
import com.me.postfetcher.database.model.createUser
import com.me.postfetcher.database.model.findUserBySub
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.header
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun Route.authenticateUser(
    vararg configurations: String? = arrayOf("auth0"),
    optional: Boolean = false,
    build: Route.() -> Unit
): Route {
    val authRoute = authenticate(*configurations, optional = optional) { build() }
    authRoute.intercept(ApplicationCallPipeline.Plugins) {
        val user = call.ensureUserExists()
        call.attributes.put(UserKey, user)
    }

    return authRoute
}

val UserKey = AttributeKey<User>("User")

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

suspend fun ApplicationCall.ensureUserExists(): User {
    val principal = authentication.principal<JWTPrincipal>()
    val sub = principal?.payload?.subject ?: throw Exception("No sub found in JWT")

    return findUserBySub(sub) ?: createUserFromAuthProfile(accessTokenFromCall(this))
}

suspend fun createUserFromAuthProfile(accessToken: String): User = createUser(getUserInfoFromToken(accessToken))


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
