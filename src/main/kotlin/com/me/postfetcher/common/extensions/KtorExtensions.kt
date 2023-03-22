package com.me.postfetcher.common.extensions

import arrow.core.Either
import com.me.postfetcher.AppError
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

fun <T : Any> Either<AppError, T>.toApiResponse(
    successStatusCode: HttpStatusCode? = null,
    ifLeft: (AppError) -> ApiResponse = { resolveLeft(it) },
    ifRight: (T) -> ApiResponse = { resolveRight(it, successStatusCode) }
): ApiResponse = fold(ifLeft = ifLeft, ifRight = ifRight)

suspend fun ApplicationCall.apiResponse(apiResponse: ApiResponse) {
    when (apiResponse) {
        is ApiResponse.ErrorResponse -> respond(apiResponse.status, apiResponse.message)
        is ApiResponse.BodyResponse -> respond(apiResponse.status, apiResponse.message)
        is ApiResponse.EmptyResponse -> respond(apiResponse.status)
    }
}

private fun resolveLeft(error: AppError) = ApiResponse.ErrorResponse(InternalServerError, error)

private fun resolveRight(body: Any, successStatusCode: HttpStatusCode?) =
    if (body is Unit) {
        ApiResponse.EmptyResponse(
            successStatusCode ?: HttpStatusCode.NoContent
        )
    } else {
        ApiResponse.BodyResponse(
            successStatusCode ?: HttpStatusCode.OK,
            body
        )
    }

sealed class ApiResponse {
    data class ErrorResponse(val status: HttpStatusCode, val message: AppError) : ApiResponse()
    data class BodyResponse(val status: HttpStatusCode, val message: Any) : ApiResponse()
    data class EmptyResponse(val status: HttpStatusCode) : ApiResponse()
}
