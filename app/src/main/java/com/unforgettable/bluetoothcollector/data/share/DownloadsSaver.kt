package com.unforgettable.bluetoothcollector.data.share

import androidx.annotation.RequiresApi
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

class DownloadsSaver {

    fun saveToDownloads(context: Context, sourceFile: File, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, sourceFile, mimeType)
        } else {
            saveViaLegacy(sourceFile)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(context: Context, sourceFile: File, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { output ->
            sourceFile.inputStream().use { input -> input.copyTo(output) }
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacy(sourceFile: File): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        val targetFile = File(downloadsDir, sourceFile.name)
        sourceFile.inputStream().use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(targetFile)
    }
}
