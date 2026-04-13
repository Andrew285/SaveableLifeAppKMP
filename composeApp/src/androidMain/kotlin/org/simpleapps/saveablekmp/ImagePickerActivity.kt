// androidMain/kotlin/org/simpleapps/saveablekmp/ImagePickerActivity.kt
package org.simpleapps.saveablekmp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class ImagePickerActivity : ComponentActivity() {

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            val bytes = uri?.let {
                contentResolver.openInputStream(it)?.readBytes()
            }
            ImagePicker.deliverResult(bytes)
        } else {
            ImagePicker.deliverResult(null)
        }
        finish()
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
        private const val EXTRA_INTENT = "extra_intent"

        fun launch(context: Context, intent: Intent) {
            val wrapper = Intent(context, ImagePickerActivity::class.java).apply {
                putExtra(EXTRA_INTENT, intent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(wrapper)
        }
    }
}