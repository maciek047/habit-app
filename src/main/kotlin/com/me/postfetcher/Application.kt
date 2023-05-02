package com.me.postfetcher

import com.auth0.jwk.JwkProviderBuilder
import com.me.postfetcher.common.dependency.Dependencies
import com.me.postfetcher.common.dependency.dependencies
import com.me.postfetcher.database.DatabaseConfig
import com.me.postfetcher.route.authRouting
import com.me.postfetcher.route.mainRouting
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

fun main() = runBlocking<Unit>(Dispatchers.Default) {

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        Netty,
        port = port,
        module = Application::module
    ).start(wait = true)
}

data class UserSession(val userId: String)

data class AuthConfig(
    val domain: String,
    val clientId: String,
    val clientSecret: String,
    val audience: String,
    val callbackUrl: String,
    val sessionSignKey: String
)


fun Application.setup(dep: Dependencies) {


    val domain = System.getenv("AUTH0_DOMAIN")
    val clientId = System.getenv("AUTH0_CLIENT_ID")
    println("clientId: $clientId")
    val clientSecret = System.getenv("AUTH0_CLIENT_SECRET")
    val audience = System.getenv("AUTH0_AUDIENCE")
    val callbackUrl = System.getenv("AUTH0_CALLBACK_URL")
    val sessionSignKey = System.getenv("SESSION_SIGN_KEY")

    val authConfig = AuthConfig(
        domain = domain,
        clientId = clientId,
        clientSecret = clientSecret,
        audience = audience,
        callbackUrl = callbackUrl,
        sessionSignKey = sessionSignKey
    )


    install(Sessions) {
        cookie<UserSession>("user_session_cookie") {
            val secretSignKey = hex(sessionSignKey)
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
            cookie.path = "/"
            cookie.extensions["SameSite"] = "none"
            cookie.httpOnly = true
            cookie.secure = true
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // 1 week
        }
    }

    val jwkProvider = JwkProviderBuilder("https://$domain/")
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    fun validateCreds(credential: JWTCredential): JWTPrincipal? {
        val containsAudience = credential.payload.audience.contains(System.getenv("AUTH0_AUDIENCE"))
        println("credential.payload.audience: ${credential.payload.audience.joinToString()}")

        if (containsAudience) {
            return JWTPrincipal(credential.payload)
        }

        return null
    }



    install(Authentication) {
        jwt("auth0") {
            verifier(jwkProvider, System.getenv("AUTH0_AUDIENCE"))
            validate { credential -> validateCreds(credential) }
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
////                    .withAudience(jwtAudience) //todo
//                    .build()
//            )
//            validate { credential ->
//                println("validating credentials")
//                println("expires at: ${credential.expiresAt}")
//                println("subject: ${credential.payload.subject}")
//                println("email claim ${credential.payload.getClaim("email").asString()}")
//                println("credential.payload: ${credential.payload}")
//                if (credential.payload.getClaim("email_verified").asBoolean()) {
//                    JWTPrincipal(credential.payload)
//                } else {
//                    null
//                }
//            }
//        }
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            encodeDefaults = true
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
        allowSameOrigin = true
//        anyHost() // @TODO Fix for production.
    }

    routing {
        authRouting(authConfig)
        mainRouting(authConfig)
    }
    runBlocking {
        DatabaseConfig.connect()
    }
}

fun Application.module() {
    install(XForwardedHeaders)
    install(ForwardedHeaders)
    setup(dependencies())
}
