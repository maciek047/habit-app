package com.me.postfetcher.route

inline fun ensure(message: String, predicate: () -> Boolean) {
    if (!predicate()) {
        throw ValidationException(message)
    }
}

class ValidationException(message: String) : RuntimeException(message)
