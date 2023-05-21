package com.me.postfetcher

sealed interface AppError {
  data class FileServiceError(val description: String, val error: Throwable) : AppError
  data class ClientError(val description: String, val error: Throwable) : AppError

  data class ValidationError(val description: String) : AppError

  data class Unauthorized(val description: String, val error: Throwable) : AppError
}
