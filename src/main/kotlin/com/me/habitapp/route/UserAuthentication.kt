package com.me.habitapp.route


import com.me.habitapp.UserSession
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

//todo to be implemented later
val UserAuthentication = createApplicationPlugin(
    name = "UserAuthentication"
) {
    onCall { call ->
        val userSession = call.sessions.get<UserSession>()
        if (userSession == null) {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

