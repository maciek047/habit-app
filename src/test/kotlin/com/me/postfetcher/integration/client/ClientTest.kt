package com.me.postfetcher.integration.client

import com.me.postfetcher.common.dependency.dependencies
import com.me.postfetcher.domain.Post
import com.me.postfetcher.service.GET_POSTS_BASE_URL
import com.me.postfetcher.service.GET_POSTS_SUFFIX
import com.me.postfetcher.setup
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication

class ClientTest : StringSpec({

    val dependencies = dependencies()

    "should get list of given objects from http call response" {
        testApplication {
            application { setup(dependencies) }

            val testObj = dependencies.client

            val result = testObj.getObjectList<Post>("$GET_POSTS_BASE_URL/$GET_POSTS_SUFFIX").shouldBeRight()
            result.size shouldBeGreaterThan 0
            result[0]::class shouldBe Post::class
        }
    }
})
