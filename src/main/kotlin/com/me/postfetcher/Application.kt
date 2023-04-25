package com.me.postfetcher

import com.me.postfetcher.common.dependency.Dependencies
import com.me.postfetcher.common.dependency.dependencies
import com.me.postfetcher.database.DatabaseConfig
import com.me.postfetcher.route.authRouting
import com.me.postfetcher.route.mainRouting
import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking<Unit>(Dispatchers.Default) {

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        Netty,
        port = port,
        module = Application::module
    ).start(wait = true)
}

data class UserSession(val userId: String)


fun Application.setup(dep: Dependencies) {

    val domain = System.getenv("AUTH0_DOMAIN")
    val clientId = System.getenv("AUTH0_CLIENT_ID")
    val clientSecret = System.getenv("AUTH0_CLIENT_SECRET")
    val audience = System.getenv("AUTH0_AUDIENCE")
    val callbackUrl = System.getenv("AUTH0_CALLBACK_URL")
    val sessionSignKey = System.getenv("SESSION_SIGN_KEY")

//    install(UserAuthentication)

    install(Authentication) {
        oauth("auth0") {
            client = HttpClient()
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "auth0",
                    authorizeUrl = "https://$domain/authorize",
                    accessTokenUrl = "https://$domain/oauth/token",
                    clientId = clientId,
                    clientSecret = clientSecret,
                    defaultScopes = listOf("openid", "profile", "email")
                )
            }
            urlProvider = { "$callbackUrl" }
        }

//        jwt("jwtAuth") {
//            val jwtIssuer = "https://$domain/"
//            val jwtAudience = audience
//
//            realm = "ktor jwtAuth"
//            verifier(
//                JWT
//                    .require(Algorithm.HMAC256(clientSecret))
//                    .withIssuer(jwtIssuer)
//                    .withAudience(jwtAudience) //todo
//                    .build()
//            )
//            validate { credential ->
//                if (credential.payload.getClaim("email_verified").asBoolean()) {
//                    JWTPrincipal(credential.payload)
//                } else {
//                    null
//                }
//            }
//        }
    }


    install(Sessions) {
        cookie<UserSession>("user_session_cookie") {
            val secretSignKey = hex(sessionSignKey)
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
            cookie.httpOnly = true
            cookie.secure = true
            cookie.domain = "shrouded-plains-88631.herokuapp.com" // Replace this with your domain
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // 1 week

        }
    }

    install(ContentNegotiation) {
        json(Json {
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        allowHost("sleepy-spire-13018.herokuapp.com", schemes = listOf("https"))
        allowHost("localhost:3000", schemes = listOf("http", "https"))
        allowHost("localhost:8000", schemes = listOf("http", "https"))
//        allowSameOrigin = true
//        anyHost() // @TODO Fix for production.
    }

    routing {
        authRouting(domain, clientId, callbackUrl)
        mainRouting()
    }
    runBlocking {
        DatabaseConfig.connect()
    }
}

fun Application.module() {
    setup(dependencies())
}
