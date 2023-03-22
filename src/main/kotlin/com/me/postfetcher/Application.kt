package com.me.postfetcher

import com.me.postfetcher.common.dependency.Dependencies
import com.me.postfetcher.common.dependency.dependencies
import com.me.postfetcher.route.mainRouting
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking<Unit>(Dispatchers.Default) {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
    ) {
        setup(dependencies())
    }
        .start(wait = true)
}

fun Application.setup(dep: Dependencies) {
    install(ContentNegotiation) {
        json(Json {
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    routing {
        mainRouting(dep.postsFetcher)
    }
}
