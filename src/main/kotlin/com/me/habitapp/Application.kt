package com.me.habitapp

import com.auth0.jwk.JwkProviderBuilder
import com.me.habitapp.common.dependency.Dependencies
import com.me.habitapp.common.dependency.dependencies
import com.me.habitapp.database.DatabaseConfig
import com.me.habitapp.route.mainRouting
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

fun validateCreds(credential: JWTCredential): JWTPrincipal? {
    val containsAudience = credential.payload.audience.contains(System.getenv("AUTH0_AUDIENCE"))
    println("credential.payload.audience: ${credential.payload.audience.joinToString()}")

    if (containsAudience) {
        return JWTPrincipal(credential.payload)
    }

    return null
}

fun Application.setup(dep: Dependencies) {

    val domain = System.getenv("AUTH0_DOMAIN")
    val clientId = System.getenv("AUTH0_CLIENT_ID")
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

    val jwkProvider = JwkProviderBuilder("https://$domain/")
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()


    install(Authentication) {
        jwt("auth0") {
            verifier(jwkProvider, "https://$domain/")
            validate { credential -> validateCreds(credential) }
        }
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
        allowHost("www.dailyhabitsmaster.com", schemes = listOf("https"))
        allowHost("localhost:3000", schemes = listOf("http", "https"))
        allowHost("localhost:3000/habits", schemes = listOf("http", "https"))
        allowHost("localhost:8000", schemes = listOf("http", "https"))
        allowSameOrigin = true
        allowNonSimpleContentTypes = true
    }

    routing {
        mainRouting()
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
