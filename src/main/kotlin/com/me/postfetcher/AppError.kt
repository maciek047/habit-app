package com.me.postfetcher

sealed interface AppError {
  data class FileServiceError(val description: String, val error: Throwable) : AppError
  data class ClientError(val description: String, val error: Throwable) : AppError
}
