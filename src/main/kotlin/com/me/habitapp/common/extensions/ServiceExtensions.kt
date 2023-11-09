package com.me.habitapp.common.extensions

import java.time.LocalDate

fun String.toLocalDate(): LocalDate = LocalDate.parse(this)
