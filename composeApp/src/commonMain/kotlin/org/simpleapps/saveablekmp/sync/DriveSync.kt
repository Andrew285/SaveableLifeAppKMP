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
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.repository.SaveableRepository

// commonMain/sync/DriveSync.kt
class DriveSync(
    private val repository: SaveableRepository,
    private val httpClient: HttpClient,
) {
    private var accessToken: String = ""
    private var folderId: String? = null

    fun getToken() = accessToken

    fun setToken(token: String) { accessToken = token }

    // ── Знайти або створити папку SaveableApp ────────────────────────────
    suspend fun getOrCreateFolder(): String {
        if (accessToken.isBlank()) {
            throw IllegalStateException("Not signed in. Call setToken() first.")
        }

        val response = httpClient.get(
            "https://www.googleapis.com/drive/v3/files"
        ) {
            header("Authorization", "Bearer $accessToken")
            parameter("q", "name='SaveableApp' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            parameter("fields", "files(id,name)")
        }

        println("=== getOrCreateFolder status: ${response.status}")
        val bodyText = response.bodyAsText()
        println("=== getOrCreateFolder body: $bodyText")

        val body = Json.parseToJsonElement(bodyText).jsonObject
        val files = body["files"]?.jsonArray

        return if (!files.isNullOrEmpty()) {
            files[0].jsonObject["id"]!!.jsonPrimitive.content
        } else {
            createFolder()
        }
    }

    private suspend fun createFolder(): String {
        println("=== createFolder: accessToken = '$accessToken'")

        if (accessToken.isBlank()) {
            throw IllegalStateException("Access token is empty. Please sign in first.")
        }

        val response = httpClient.post(
            "https://www.googleapis.com/drive/v3/files"
        ) {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("name", "SaveableApp")
                put("mimeType", "application/vnd.google-apps.folder")
            })
        }

        println("=== createFolder response status: ${response.status}")
        val bodyText = response.bodyAsText()
        println("=== createFolder response body: $bodyText")

        val body = Json.parseToJsonElement(bodyText).jsonObject
        return body["id"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No 'id' in response: $bodyText")
    }

    // ── Завантажити items.json на Drive ──────────────────────────────────
    suspend fun pushItems() {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val unsynced = repository.getUnsyncedItems()
        if (unsynced.isEmpty()) return

        val allItems = repository.getAllItemsIncludingDeleted()
        val editedItem = allItems.find { !it.isSynced }
        println("=== push: unsynced item ${editedItem?.id?.take(6)}, updatedAt=${editedItem?.updatedAt}")

        val json = Json.encodeToString<List<SavedItem>>(allItems)
        val bytes = json.encodeToByteArray()

        val existingId = findFile("items.json", folder)

        if (existingId != null) {
            // Використовуємо multipart update замість media-only
            val boundary = "boundary_saveable_${System.currentTimeMillis()}"
            val metadata = """{"name":"items.json"}"""
            val sb = StringBuilder()
            sb.append("--$boundary\r\n")
            sb.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            sb.append("$metadata\r\n")
            sb.append("--$boundary\r\n")
            sb.append("Content-Type: application/json\r\n\r\n")
            val header = sb.toString().encodeToByteArray()
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
            println("=== patch status: ${response.status}")
            println("=== patch body: ${response.bodyAsText()}")
        } else {
            uploadFile("items.json", bytes, "application/json", folder)
        }

        unsynced.forEach { repository.markSynced(it.id) }
        repository.purgeDeletedSynced()
    }

    // ── Завантажити зображення ───────────────────────────────────────────
    suspend fun pushImage(itemId: String, imageBytes: ByteArray): String {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val fileName = "img_$itemId.jpg"
        val existingId = findFile(fileName, folder)

        return if (existingId != null) {
            httpClient.patch(
                "https://www.googleapis.com/upload/drive/v3/files/$existingId"
            ) {
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

    // ── Отримати дані з Drive ────────────────────────────────────────────
    suspend fun pullItems(): List<SavedItem> {
        val folder = folderId ?: getOrCreateFolder().also { folderId = it }
        val fileId = findFile("items.json", folder) ?: return emptyList()

        val response = httpClient.get(
            "https://www.googleapis.com/drive/v3/files/$fileId"
        ) {
            header("Authorization", "Bearer $accessToken")
            parameter("alt", "media")
        }
        return Json.decodeFromString(response.body<String>())
    }

    // ── Завантажити зображення за Drive ID ───────────────────────────────
    suspend fun pullImage(driveFileId: String): ByteArray? {
        return try {
            val response = httpClient.get(
                "https://www.googleapis.com/drive/v3/files/$driveFileId"
            ) {
                header("Authorization", "Bearer $accessToken")
                parameter("alt", "media")
            }
            response.body<ByteArray>()
        } catch (e: Exception) { null }
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private suspend fun findFile(name: String, folderId: String): String? {
        val response = httpClient.get(
            "https://www.googleapis.com/drive/v3/files"
        ) {
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
            // Multipart: metadata + file
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
        val sb = StringBuilder()
        sb.append("--$boundary\r\n")
        sb.append("Content-Type: application/json\r\n\r\n")
        sb.append("$metadata\r\n")
        sb.append("--$boundary\r\n")
        sb.append("Content-Type: $mimeType\r\n\r\n")
        val header = sb.toString().encodeToByteArray()
        val footer = "\r\n--$boundary--\r\n".encodeToByteArray()
        return header + bytes + footer
    }
}