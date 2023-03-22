package com.me.postfetcher.service

import arrow.core.Either
import arrow.core.continuations.either
import com.google.gson.Gson
import com.me.postfetcher.AppError.FileServiceError
import com.me.postfetcher.common.extensions.getByPropertyName
import com.me.postfetcher.common.extensions.validatePathSeparators
import java.io.File

class FileService {

    suspend inline fun <reified T> saveObjectsToFiles(
        objects: List<T>,
        baseDirectory: String? = null,
        filePath: String,
        fileNameFromProperty: String = "id",
        fileExtension: String
    ): Either<FileServiceError, String> =
        either {
            val filePath = filePathResolver(baseDirectory, filePath)

            objects.forEachIndexed { counter, item ->
                val fileName = item.getByPropertyName(fileNameFromProperty) ?: counter
                val finalPath = "$filePath\\$fileName$fileExtension"
                File(finalPath).writeText(Gson().toJson(item))
            }
            filePath
        }

    fun filePathResolver(baseDirectory: String?, filePath: String): String {
        val directory: String = baseDirectory ?: getTempFileDirectory()
        val finalPath = File("$directory\\$filePath\\".validatePathSeparators())
        if (!finalPath.exists()) {
            finalPath.mkdir()
        }
        return finalPath.absolutePath
    }

    private fun getTempFileDirectory(): String = System.getProperty("java.io.tmpdir")
}
