package com.me.postfetcher.arbs

import com.me.postfetcher.common.extensions.removeSquareBrackets
import com.me.postfetcher.domain.Comment
import com.me.postfetcher.domain.Post
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.arbitrary.string

fun Arb.Companion.post() = arbitrary {
    Post(
        id = Arb.positiveInt().bind(),
        userId = Arb.positiveInt().bind(),
        title = Arb.string().bind().removeSquareBrackets(),
        body = Arb.string().bind().removeSquareBrackets(),
        comments = Arb.list(Arb.comment()).bind()
    )
}

fun Arb.Companion.comment() = arbitrary {
    Comment(
        id = Arb.positiveInt().bind(),
        postId = Arb.positiveInt().bind(),
        name = Arb.string().bind().removeSquareBrackets(),
        email = Arb.string().bind().removeSquareBrackets(),
        body = Arb.string().bind().removeSquareBrackets(),
    )
}
