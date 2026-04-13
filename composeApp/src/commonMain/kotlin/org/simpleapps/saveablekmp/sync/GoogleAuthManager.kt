package org.simpleapps.saveablekmp.sync

expect class GoogleAuthManager {
    suspend fun signIn(): String? // повертає access token
    fun isSignedIn(): Boolean
    fun getSavedToken(): String?
    suspend fun signOut()
}