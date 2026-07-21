package com.eray.muhasebeapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidUrlAcici(private val context: Context) : UrlAcici {
    override fun ac(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
actual fun rememberUrlAcici(): UrlAcici {
    val context = LocalContext.current
    return remember { AndroidUrlAcici(context) }
}