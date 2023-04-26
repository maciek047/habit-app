package com.me.postfetcher.route


import arrow.core.continuations.either
import com.auth0.Tokens
import com.me.postfetcher.AppError
import com.me.postfetcher.UserSession
import com.me.postfetcher.common.extensions.apiResponse
import com.me.postfetcher.common.extensions.toApiResponse
import com.me.postfetcher.database.model.createUserIfNotExists
import com.me.postfetcher.database.model.fetchHabitsWithPlannedDays
import com.me.postfetcher.route.dto.WeeklyHabitsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.servlet.http.HttpServletRequest

fun Route.authRouting(
    domain: String?,
    clientId: String?,
    clientSecret: String?,
    callbackUrl: String?,
    sessionSignKey: String
) {

    val logger = mu.KotlinLogging.logger {}


    get("/habits") {
        authenticate { userSession ->
            logger.info("fetching habits for user ${userSession.userId}")
            val userId = UUID.fromString(userSession.userId)
            val response =
                either<AppError, WeeklyHabitsResponse> {
                    WeeklyHabitsResponse(fetchHabitsWithPlannedDays(userId))
                }.toApiResponse(HttpStatusCode.OK)
            call.apiResponse(response)
        }
    }

    get("/callback") {
        val code = call.parameters["code"] ?: error("No code received")
        val httpClient = HttpClient(CIO)
        val tokenUrl = "https://$domain/oauth/token"


        val tokenResponse: String = httpClient.post(tokenUrl) {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                "grant_type": "authorization_code",
                "client_id": "$clientId",
                "client_secret": "$clientSecret",
                "code": "$code",
                "redirect_uri": "$callbackUrl"
                }
            """.trimIndent()
            )
        }.body()


        val tokenResponseBody = Json.parseToJsonElement(tokenResponse).jsonObject
        val accessToken = tokenResponseBody["access_token"]?.jsonPrimitive?.content

        println("accessToken: $accessToken")

        val userInfoUrl = "https://$domain/userinfo"
        println("userInfoUrl: $userInfoUrl")
        val userInfoResponse: String = httpClient.get(userInfoUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }.body()

        val userInfoResponseBody = Json.parseToJsonElement(userInfoResponse).jsonObject
        val userEmail = userInfoResponseBody["email"]?.jsonPrimitive?.content

        val user = createUserIfNotExists(userEmail!!)

        logger.info("userId: ${user.id}")

        val userSession = UserSession(user.id.toString())
        call.sessions.set("user_session_cookie", userSession)
        println("userSession set!!")
        call.respondRedirect("https://sleepy-spire-13018.herokuapp.com/habits")
    }


    get("/login") {
        val auth0Url = "https://$domain/authorize?" +
                "response_type=code&" +
                "client_id=$clientId&" +
                "redirect_uri=$callbackUrl&" +
                "scope=openid%20profile%20email"
        call.respondRedirect(auth0Url)
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        val logoutUrl = "https://$domain/v2/logout?" +
                "client_id=$clientId&" +
                "returnTo=https://sleepy-spire-13018.herokuapp.com/"
        call.respondRedirect(logoutUrl)
    }

}

const val KEY_EXPIRES_IN = "expires_in"
const val KEY_ACCESS_TOKEN = "access_token"
const val KEY_ID_TOKEN = "id_token"
const val KEY_TOKEN_TYPE = "token_type"
const val KEY_TOKEN = "token"
fun getFrontChannelTokens(request: HttpServletRequest): Tokens {
    val expiresIn = if (request.getParameter(KEY_EXPIRES_IN) == null) null else request.getParameter(
        KEY_EXPIRES_IN
    ).toLong()
    return Tokens(
        request.getParameter(KEY_ACCESS_TOKEN),
        request.getParameter(KEY_ID_TOKEN),
        null,
        request.getParameter(KEY_TOKEN_TYPE),
        expiresIn
    )
}