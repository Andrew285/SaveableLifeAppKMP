package org.simpleapps.saveablekmp.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.repository.SaveableRepository

class DriveSync(
    private val repository: SaveableRepository,
    private val httpClient: HttpClient,
) {
    private var accessToken: String = ""
    private var folderId: String? = null

    fun getToken() = accessToken
    fun setToken(token: String) { accessToken = token }

    // ── Folder ───────────────────────────────────────────────────────────

    suspend fun getOrCreateFolder(): String {
        if (accessToken.isBlank()) throw IllegalStateException("Not signed in.")

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header("Authorization", "Bearer $accessToken")
            parameter("q", "name='SaveableApp' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            parameter("fields", "files(id,name)")
        }

        val bodyText = response.bodyAsText()
        val body = Json.parseToJsonElement(bodyText).jsonObject
        val files = body["files"]?.jsonArray

        return if (!files.isNullOrEmpty()) {
            files[0].jsonObject["id"]!!.jsonPrimitive.content
        } else {
            createFolder()
        }
    }

    private suspend fun createFolder(): String {
        if (accessToken.isBlank()) throw IllegalStateException("Access token is empty.")

        val response = httpClient.post("https://www.googleapis.com/drive/v3/files") {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("name", "SaveableApp")
                put("mimeType", "application/vnd.google-apps.folder")
            })
        }

        val bodyText = response.bodyAsText()
        val body = Json.parseToJsonElement(bodyText).jsonObject
        return body["id"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No 'id' in response: $bodyText")
    }

    // ── Items ────────────────────────────────────────────────────────────

    suspend fun pushItems() {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val unsynced = repository.getUnsyncedItems()
        if (unsynced.isEmpty()) return

        val allItems = repository.getAllItemsIncludingDeleted()
        val json = Json.encodeToString<List<SavedItem>>(allItems)

        pushJsonFile("items.json", json, folder)

        unsynced.forEach { repository.markSynced(it.id) }
        repository.purgeDeletedSynced()
    }

    suspend fun pullItems(): List<SavedItem> {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val json = pullJsonFile("items.json", folder) ?: return emptyList()
        return Json.decodeFromString(json)
    }

    // ── Categories ───────────────────────────────────────────────────────

    suspend fun pushCategories(categories: List<Category>) {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val json = Json.encodeToString<List<Category>>(categories)
        pushJsonFile("categories.json", json, folder)
    }

    suspend fun pullCategories(): List<Category> {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val json = pullJsonFile("categories.json", folder) ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            println("=== pullCategories decode error: ${e.message}")
            emptyList()
        }
    }

    // ── Images ───────────────────────────────────────────────────────────

    suspend fun pushImage(itemId: String, imageBytes: ByteArray): String {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val fileName = "img_$itemId.jpg"
        val existingId = findFile(fileName, folder)

        return if (existingId != null) {
            httpClient.patch("https://www.googleapis.com/upload/drive/v3/files/$existingId") {
                header("Authorization", "Bearer $accessToken")
                parameter("uploadType", "media")
                contentType(ContentType.Image.JPEG)
                setBody(imageBytes)
            }
            "drive://$existingId"
        } else {
            val id = uploadFile(fileName, imageBytes, "image/jpeg", folder)
            "drive://$id"
        }
    }

    suspend fun pullImage(driveFileId: String): ByteArray? {
        return try {
            val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$driveFileId") {
                header("Authorization", "Bearer $accessToken")
                parameter("alt", "media")
            }
            response.body<ByteArray>()
        } catch (e: Exception) { null }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Завантажує або оновлює JSON-файл на Drive.
     * Якщо файл вже існує — робить PATCH (multipart update щоб зберегти metadata).
     * Якщо не існує — створює через POST multipart.
     */
    private suspend fun pushJsonFile(fileName: String, json: String, folderId: String) {
        val bytes = json.encodeToByteArray()
        val existingId = findFile(fileName, folderId)

        if (existingId != null) {
            val boundary = "boundary_saveable_${System.currentTimeMillis()}"
            val metadata = """{"name":"$fileName"}"""
            val header = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append("$metadata\r\n")
                append("--$boundary\r\n")
                append("Content-Type: application/json\r\n\r\n")
            }.encodeToByteArray()
            val footer = "\r\n--$boundary--".encodeToByteArray()
            val body = header + bytes + footer

            val response = httpClient.patch(
                "https://www.googleapis.com/upload/drive/v3/files/$existingId"
            ) {
                header("Authorization", "Bearer $accessToken")
                parameter("uploadType", "multipart")
                contentType(ContentType.parse("multipart/related; boundary=$boundary"))
                setBody(body)
            }
            println("=== patch $fileName status: ${response.status}")
        } else {
            uploadFile(fileName, bytes, "application/json", folderId)
            println("=== uploaded new $fileName")
        }
    }

    /**
     * Читає JSON-файл з Drive. Повертає null якщо файл не знайдено.
     */
    private suspend fun pullJsonFile(fileName: String, folderId: String): String? {
        val fileId = findFile(fileName, folderId) ?: return null

        val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
            header("Authorization", "Bearer $accessToken")
            parameter("alt", "media")
        }
        return response.body<String>()
    }

    private suspend fun findFile(name: String, folderId: String): String? {
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header("Authorization", "Bearer $accessToken")
            parameter("q", "'$folderId' in parents and name='$name' and trashed=false")
            parameter("fields", "files(id)")
        }
        val files = response.body<JsonObject>()["files"]?.jsonArray
        return files?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
    }

    private suspend fun uploadFile(
        name: String,
        bytes: ByteArray,
        mimeType: String,
        folderId: String,
    ): String {
        val response = httpClient.post(
            "https://www.googleapis.com/upload/drive/v3/files"
        ) {
            header("Authorization", "Bearer $accessToken")
            parameter("uploadType", "multipart")
            val boundary = "boundary_saveable"
            contentType(ContentType.parse("multipart/related; boundary=$boundary"))
            setBody(buildMultipart(boundary, name, mimeType, folderId, bytes))
        }
        return response.body<JsonObject>()["id"]!!.jsonPrimitive.content
    }

    private fun buildMultipart(
        boundary: String,
        name: String,
        mimeType: String,
        folderId: String,
        bytes: ByteArray,
    ): ByteArray {
        val metadata = """{"name":"$name","parents":["$folderId"]}"""
        val header = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append("$metadata\r\n")
            append("--$boundary\r\n")
            append("Content-Type: $mimeType\r\n\r\n")
        }.encodeToByteArray()
        val footer = "\r\n--$boundary--\r\n".encodeToByteArray()
        return header + bytes + footer
    }
}