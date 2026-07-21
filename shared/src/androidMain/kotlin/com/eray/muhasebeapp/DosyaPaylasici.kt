package com.eray.muhasebeapp

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

private fun mimeTypeIcin(dosyaAdi: String): String = when {
    dosyaAdi.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    dosyaAdi.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
    dosyaAdi.endsWith(".csv", ignoreCase = true) -> "text/csv"
    dosyaAdi.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    else -> "text/plain"
}

class AndroidDosyaPaylasici(private val context: Context) : DosyaPaylasici {

    override fun paylas(dosyaAdi: String, icerik: String) {
        yaz(dosyaAdi, icerik.toByteArray(Charsets.UTF_8))
    }

    override fun paylasBytes(dosyaAdi: String, icerik: ByteArray) {
        yaz(dosyaAdi, icerik)
    }

    private fun yaz(dosyaAdi: String, icerik: ByteArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ : MediaStore ile doğrudan İndirilenler klasörüne yaz
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, dosyaAdi)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeIcin(dosyaAdi))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        stream.write(icerik)
                    }
                    Toast.makeText(context, "Rapor İndirilenler klasörüne kaydedildi", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(context, "Rapor kaydedilemedi", Toast.LENGTH_LONG).show()
                }
            } else {
                // Android 9 ve altı : Doğrudan Downloads klasörüne dosya yazma
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dosya = File(downloadsDir, dosyaAdi)
                FileOutputStream(dosya).use { stream ->
                    stream.write(icerik)
                }
                Toast.makeText(context, "Rapor İndirilenler klasörüne kaydedildi", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
actual fun rememberDosyaPaylasici(): DosyaPaylasici {
    val context = LocalContext.current
    return remember { AndroidDosyaPaylasici(context) }
}
