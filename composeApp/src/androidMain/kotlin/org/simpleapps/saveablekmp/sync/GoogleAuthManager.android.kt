package org.simpleapps.saveablekmp.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.resume

actual class GoogleAuthManager : KoinComponent {
    private val context: Context by inject()

    private val prefs by lazy {
        context.getSharedPreferences("saveable_prefs", Context.MODE_PRIVATE)
    }

    private fun buildGso() = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestEmail()
        .build()

    actual suspend fun signIn(): String? {
        // Спочатку перевір чи вже залогінений
        val existing = GoogleSignIn.getLastSignedInAccount(context)
        if (existing != null) {
            val token = getAccessToken(existing)
            if (token != null) {
                prefs.edit().putString(KEY_TOKEN, token).apply()
                return token
            }
        }

        // Якщо ні — запускаємо Sign In flow
        return suspendCancellableCoroutine { cont ->
            pendingContinuation = cont
            val client = GoogleSignIn.getClient(context, buildGso())
            SignInActivity.launch(context, client.signInIntent)
        }
    }

    actual fun isSignedIn(): Boolean =
        prefs.getString(KEY_TOKEN, null) != null ||
                GoogleSignIn.getLastSignedInAccount(context) != null

    actual suspend fun signOut() {
        prefs.edit().remove(KEY_TOKEN).apply()
        withContext(Dispatchers.IO) {
            GoogleSignIn.getClient(context, buildGso()).signOut()
        }
    }

    actual fun getSavedToken(): String? = prefs.getString(KEY_TOKEN, null)

    suspend fun deliverResult(account: GoogleSignInAccount?) {
        if (account == null) {
            pendingContinuation?.resume(null)
            pendingContinuation = null
            return
        }
        // Отримуємо реальний accessToken через GoogleAuthUtil
        val token = getAccessToken(account)
        println("=== deliverResult token: $token")
        if (token != null) {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
        pendingContinuation?.resume(token)
        pendingContinuation = null
    }

    private suspend fun getAccessToken(account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:${DriveScopes.DRIVE_FILE}"
                )
            } catch (e: Exception) {
                println("=== getAccessToken error: ${e.message}")
                null
            }
        }
    }


    companion object {
        // Отримай Web Client ID з Google Cloud Console →
        // APIs & Services → Credentials → OAuth 2.0 Client IDs → Web Client
        const val WEB_CLIENT_ID = "284002897406-20qv40kl91sohdtkn46hnl8hp888vkgu.apps.googleusercontent.com"
        private const val KEY_TOKEN = "google_access_token"
        var pendingContinuation: kotlin.coroutines.Continuation<String?>? = null
    }
}