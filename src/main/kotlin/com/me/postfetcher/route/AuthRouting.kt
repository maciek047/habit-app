package com.me.postfetcher.route

import com.me.postfetcher.AuthConfig
import io.ktor.server.routing.Route

fun Route.authRouting(authConfig: AuthConfig) {

    val logger = mu.KotlinLogging.logger {}


    val domain = authConfig.domain
    val clientId = authConfig.clientId
    val clientSecret = authConfig.clientSecret
    val callbackUrl = authConfig.callbackUrl




}