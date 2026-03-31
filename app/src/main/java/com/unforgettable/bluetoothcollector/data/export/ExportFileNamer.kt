package com.unforgettable.bluetoothcollector.data.export

import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ExportFileNamer(
    private val timestampFormatter: DateTimeFormatter = FILE_TIMESTAMP_FORMATTER,
    private val fileWriter: (File, ByteArray) -> Unit = { file, bytes -> file.writeBytes(bytes) },
) {

    fun writeExportFile(
        directory: File,
        sessionId: String,
        format: ExportFormat,
        exportedAt: OffsetDateTime,
        content: ByteArray,
    ): File {
        require(directory.exists() || directory.mkdirs()) { "Unable to create export directory: $directory" }
        require(directory.isDirectory) { "Export path is not a directory: $directory" }

        val baseName = "session-${sanitize(sessionId)}-${exportedAt.format(timestampFormatter)}"
        val extension = format.name.lowercase()
        var suffix = 0
        while (true) {
            val candidateName = if (suffix == 0) {
                "$baseName.$extension"
            } else {
                "$baseName-$suffix.$extension"
            }
            val candidate = File(directory, candidateName)
            if (candidate.createNewFile()) {
                return try {
                    fileWriter(candidate, content)
                    candidate
                } catch (writeFailure: IOException) {
                    candidate.delete()
                    throw writeFailure
                } catch (writeFailure: RuntimeException) {
                    candidate.delete()
                    throw writeFailure
                }
            }
            suffix += 1
        }
    }

    private fun sanitize(sessionId: String): String {
        return sessionId.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private companion object {
        val FILE_TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssxx")
    }
}
