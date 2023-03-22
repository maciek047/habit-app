package com.me.postfetcher.common.extensions

import java.util.Locale

inline fun <reified T> T.getByPropertyName(propertyName: String): Any? {
    val getterName = "get" + propertyName.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
    return T::class.java.getMethod(getterName).invoke(this)
}

fun String.validatePathSeparators(): String {
    return replace(REGEX_MULTIPLE_SEPARATORS.toRegex(), "\\\\")
}

fun String.removeSquareBrackets(): String {
    return replace("[","").replace("]", "")
}

fun String.isJsonArray(): Boolean = REGEX_JSON_ARRAY.toRegex().matches(this)

const val REGEX_JSON_ARRAY = """\[\s*([^\[\]]*?)\s*\]"""
const val REGEX_MULTIPLE_SEPARATORS = """\\\\+"""
