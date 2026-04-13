package org.simpleapps.saveablekmp.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SignInActivity : ComponentActivity() {

    private val authManager: GoogleAuthManager by inject()

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val account = GoogleSignIn
                        .getSignedInAccountFromIntent(result.data).result
                    authManager.deliverResult(account)
                } catch (e: Exception) {
                    println("=== SignInActivity error: ${e.message}")
                    authManager.deliverResult(null)
                }
            } else {
                authManager.deliverResult(null)
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = this.intent.getParcelableExtra<Intent>(EXTRA_INTENT)
        if (intent != null) {
            launcher.launch(intent)
        } else {
            finish()
        }
    }

    companion object {
        private const val EXTRA_INTENT = "extra_sign_in_intent"

        fun launch(context: Context, intent: Intent) {
            val wrapper = Intent(context, SignInActivity::class.java).apply {
                putExtra(EXTRA_INTENT, intent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(wrapper)
        }
    }
}