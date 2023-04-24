package com.me.postfetcher.route


import com.auth0.json.auth.UserInfo
import com.me.postfetcher.UserSession
import com.me.postfetcher.database.model.createUserIfNotExists
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Route.authRouting(
    domain: String?,
    clientId: String?,
    callbackUrl: String?
) {

    val logger = mu.KotlinLogging.logger {}


    get("/callback") {
//        val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
//            ?: error("No principal received")

        val accessToken = call.parameters.forEach { s, strings ->
            println("s: $s, $strings")
        }

        val body = call.receive<String>()
        println("body: $body")


        logger.info("principal received correctly: $accessToken")

        // Get user profile information from the /userinfo endpoint
        val httpClient = HttpClient()
        val userInfoUrl = "https://$domain/userinfo"
        val userInfoResponse: UserInfo = httpClient.get(userInfoUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }.body()

        logger.info("userInfoResponse: $userInfoResponse")

        val user = createUserIfNotExists(userInfoResponse)

        logger.info("user: $user")
        val userSession = UserSession(user.id.toString())
        call.sessions.set(userSession)
        call.respondRedirect("/habits")
    }

//    authenticate("auth0") {
//        get("/callback") {
//            val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
//                ?: error("No principal received")
//
//            logger.info("principal received correctly: $principal")
//
//            val accessToken = principal.accessToken
//
//            // Get user profile information from the /userinfo endpoint
//            val httpClient = HttpClient()
//            val userInfoUrl = "https://$domain/userinfo"
//            val userInfoResponse: UserInfo = httpClient.get(userInfoUrl) {
//                headers {
//                    append(HttpHeaders.Authorization, "Bearer $accessToken")
//                }
//            }.body()
//
//            val user = createUserIfNotExists(userInfoResponse)
//            val userSession = UserSession(user.id.toString())
//            call.sessions.set(userSession)
//            call.respondRedirect("/habits")
//        }
//    }

    get("/login") {
        val auth0Url = "https://$domain/authorize?" +
                "response_type=token&" +
                "client_id=$clientId&" +
                "redirect_uri=$callbackUrl&" +
                "scope=openid%20profile%20email"
        call.respondRedirect(auth0Url)
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        val logoutUrl = "https://$domain/v2/logout?" +
                "client_id=$clientId&" +
                "returnTo=https://your-heroku-app-name.herokuapp.com" // todo  Replace this with your app URL
        call.respondRedirect(logoutUrl)
    }

    authenticate("jwtAuth") {
        get("/test-endpoint") {

            val principal = call.authentication.principal<JWTPrincipal>()
            println("principal: $principal")
            println("payload: ${principal?.payload}")
            println("user_id: ${principal?.payload?.getClaim("user_id")?.asString()}")
            println(principal?.audience)
            println(principal?.issuer)
            println(principal?.payload?.subject)
            println(principal?.payload?.audience)
            println(principal?.payload?.id)
            println(principal?.payload?.claims)

            // Do something with the jwtToken and userId
            // ...
        }

//        get("/is-authenticated") {
//            logger.info("is-authenticated authenticated called")
//            call.respond(HttpStatusCode.OK, mapOf("authenticated" to true))
//        }
    }


}
