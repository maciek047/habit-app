package com.me.postfetcher.route

import arrow.core.Either

fun ensure(value: Boolean, errorMessage: String): Either<ValidationException, Unit> {
    return if (!value) {
        Either.Left(ValidationException(errorMessage))
    } else {
        Either.Right(Unit)
    }
}

fun ensure(predicate: Boolean, then: () -> Unit) {
    if (!predicate) {
        then()
    }
}

class ValidationException(message: String) : RuntimeException(message)
