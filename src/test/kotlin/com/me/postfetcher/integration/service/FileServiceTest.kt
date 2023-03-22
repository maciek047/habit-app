package com.me.postfetcher.integration.service

import com.me.postfetcher.arbs.post
import com.me.postfetcher.common.dependency.dependencies
import com.me.postfetcher.setup
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.ktor.server.testing.testApplication
import java.io.File

class FileServiceTest : StringSpec({

    val dependencies = dependencies()

    "should save given objects to files" {
        testApplication {
            application { setup(dependencies) }
            //given
            val givenPath = "test2"
            val givenFileExtension = ".txt"

            val posts = listOf(
                Arb.post().next(),
                Arb.post().next(),
                Arb.post().next(),
            )

            val testObj = dependencies.fileService

            //when
            val result = testObj.saveObjectsToFiles(
                objects = posts,
                filePath = givenPath,
                fileNameFromProperty = "userId",
                fileExtension = givenFileExtension
            ).shouldBeRight()

            //then
            File("$result\\${posts[0].userId}$givenFileExtension").exists() shouldBe true
            File("$result\\${posts[1].userId}$givenFileExtension").exists() shouldBe true
            File("$result\\${posts[2].userId}$givenFileExtension").exists() shouldBe true
        }
    }
})
