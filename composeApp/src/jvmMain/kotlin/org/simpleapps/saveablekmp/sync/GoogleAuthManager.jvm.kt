package org.simpleapps.saveablekmp.sync

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.DriveScopes
import java.io.File

actual class GoogleAuthManager {
    private val saveDir = File(System.getProperty("user.home"), ".saveable")
    private val tokenFile = File(saveDir, "token.json")
    private val storedCredentialFile = File(saveDir, "StoredCredential") // ← реальний файл
    private var credential: Credential? = null

    actual fun isSignedIn(): Boolean {
        // Перевіряємо обидва варіанти
        return credential?.accessToken != null ||
                tokenFile.exists() ||
                storedCredentialFile.exists() // ← додай це
    }

    actual fun getSavedToken(): String? {
        // Якщо credential вже є в пам'яті — повертаємо
        credential?.accessToken?.let { return it }
        // StoredCredential або token.json є — потрібно відновити через signIn()
        if (storedCredentialFile.exists() || tokenFile.exists()) {
            return "__needs_refresh__"
        }
        return null
    }

    actual suspend fun signIn(): String? {
        val credentialsFile = File(saveDir, "credentials.json")
        if (!credentialsFile.exists()) {
            throw IllegalStateException(
                "credentials.json не знайдено!\n" +
                        "Поклади файл сюди: ${credentialsFile.absolutePath}"
            )
        }
        val clientSecrets = GoogleClientSecrets.load(
            GsonFactory.getDefaultInstance(),
            credentialsFile.reader()
        )
        val flow = GoogleAuthorizationCodeFlow.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            clientSecrets,
            listOf(DriveScopes.DRIVE_FILE),
        )
            .setDataStoreFactory(FileDataStoreFactory(saveDir)) // ← saveDir напряму
            .setAccessType("offline")
            .build()

        // authorize() автоматично відновить з StoredCredential без браузера
        credential = AuthorizationCodeInstalledApp(
            flow, LocalServerReceiver()
        ).authorize("user")

        // Оновлюємо токен якщо він застарів
        if (credential?.expiresInSeconds != null && credential!!.expiresInSeconds <= 60) {
            credential!!.refreshToken()
        }

        println("=== signIn accessToken: ${credential?.accessToken?.take(10)}")
        return credential?.accessToken
    }

    actual suspend fun signOut() {
        storedCredentialFile.delete()
        tokenFile.delete()
        credential = null
    }
}