package com.me.postfetcher.integration.route

import com.me.postfetcher.common.dependency.dependencies
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import com.me.postfetcher.setup
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText

class MainRoutingTest : StringSpec({

    val dependencies = dependencies()

    "should return 200 on valid call" {
        testApplication {
            application { setup(dependencies) }
            val response = client.get("/posts")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldNotBe null
        }
    }

    "should return 404 on invalid call" {
        testApplication {
            application { setup(dependencies) }
            val response = client.get("/wrongurl")

            response.status shouldBe HttpStatusCode.NotFound
        }
    }
})
