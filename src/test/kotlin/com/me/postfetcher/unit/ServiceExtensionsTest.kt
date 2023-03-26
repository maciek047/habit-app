package com.me.postfetcher.unit

import com.google.gson.Gson
import com.me.postfetcher.arbs.post
import com.me.postfetcher.common.extensions.getByPropertyName
import com.me.postfetcher.common.extensions.isJsonArray
import com.me.postfetcher.common.extensions.removeSquareBrackets
import com.me.postfetcher.common.extensions.validatePathSeparators
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next

class ServiceExtensionsTest : StringSpec({

    "should get property value by name" {
        val givenObject = Arb.post().next()

        val result = givenObject.getByPropertyName("title")

        result shouldBe givenObject.title
    }

    "should remove multiple separators from path" {
        val givenPath = "c:\\\\random\\\\\\path\\example\\\\\\file.txt"
        val expectedResult = "c:\\random\\path\\example\\file.txt"

        val result = givenPath.validatePathSeparators()

        result shouldBe expectedResult
    }


    "should remove square brackets from string" {
        val givenText = "text[] with ]n[o] [brackets]"
        val expectedResult = "text with no brackets"

        val result = givenText.removeSquareBrackets()

        result shouldBe expectedResult
    }
})
