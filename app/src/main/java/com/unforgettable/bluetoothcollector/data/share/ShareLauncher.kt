package com.unforgettable.bluetoothcollector.data.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import java.io.File

class ShareLauncher {

    fun share(
        context: Context,
        exportedFile: File,
        format: ExportFormat,
    ) {
        val shareIntent = createShareIntent(
            context = context,
            exportedFile = exportedFile,
            format = format,
        )
        val chooserIntent = Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    private fun createShareIntent(
        context: Context,
        exportedFile: File,
        format: ExportFormat,
    ): Intent {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportedFile,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private val ExportFormat.mimeType: String
        get() = when (this) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.TXT -> "text/plain"
        }
}
