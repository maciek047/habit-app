package com.me.postfetcher.route


import com.auth0.json.auth.UserInfo
import com.me.postfetcher.database.model.createUserIfNotExists
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.authRouting(
    domain: String?,
    clientId: String?,
    callbackUrl: String?
) {

    authenticate("auth0") {
//        get("/callback") {
//            val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
//                ?: error("No principal")
//
//            val accessToken = principal.accessToken
//
//            // Save the access token and user information to your session, database, or other storage
//            // ...
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
//
//            // Save user information to the PostgreSQL Users table
//            // You need to implement the following function in your code
//            // It should create a new user in the Users table if not exists, otherwise, return the existing user
//            val user = createUserIfNotExists(userInfoResponse)
//
//            // Save the user ID (UUID) in a session or a secure cookie
//            // ...
//
//            call.respondRedirect("/")
//        }

        get("/callback") {
            val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                ?: error("No principal")

            val accessToken = principal.accessToken

            // Save the access token and user information to your session, database, or other storage
            // ...

            // Get user profile information from the /userinfo endpoint
            val httpClient = HttpClient()
            val userInfoUrl = "https://$domain/userinfo"
            val userInfoResponse: UserInfo = httpClient.get(userInfoUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }.body()

            // Save user information to the PostgreSQL Users table
            // You need to implement the following function in your code
            // It should create a new user in the Users table if not exists, otherwise, return the existing user
            val user = createUserIfNotExists(userInfoResponse)

            // Save the user ID (UUID) in a session or a secure cookie
            // ...

            call.respondRedirect("/")
        }
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
        // Clear the user session or delete the access token from your storage
        // ...

        val logoutUrl = "https://$domain/v2/logout?" +
                "client_id=$clientId&" +
                "returnTo=https://your-heroku-app-name.herokuapp.com" // Replace this with your app URL
        call.respondRedirect(logoutUrl)
    }
}
