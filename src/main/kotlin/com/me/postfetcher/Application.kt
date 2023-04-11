package com.me.postfetcher

import com.me.postfetcher.common.dependency.Dependencies
import com.me.postfetcher.common.dependency.dependencies
import com.me.postfetcher.database.DatabaseConfig
import com.me.postfetcher.route.mainRouting
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
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

fun Application.setup(dep: Dependencies) {
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
        mainRouting(dep.postsFetcher)
    }
    runBlocking {
        DatabaseConfig.connect()
    }
}

fun Application.module() {
    setup(dependencies())
}
