package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import java.util.Properties

interface ImportedArtifactStoreContract {
    fun save(artifact: ImportedFileInfo)
    fun load(): ImportedFileInfo?
    fun preserveLastSuccessfulOnFailure()
    fun onCurrentSessionCleared()
}

class ImportedArtifactStore(
    private val directory: File,
) : ImportedArtifactStoreContract {
    private val metadataFile = File(directory, METADATA_FILE_NAME)

    override fun save(artifact: ImportedFileInfo) {
        directory.mkdirs()
        val properties = Properties().apply {
            setProperty(KEY_FILE_PATH, artifact.file.absolutePath)
            setProperty(KEY_SIZE_BYTES, artifact.sizeBytes.toString())
            setProperty(KEY_FORMAT, artifact.format.name)
            setProperty(KEY_RECEIVED_AT, artifact.receivedAt)
            setProperty(KEY_SOURCE_CHANNEL, artifact.sourceChannel.name)
            setProperty(KEY_FILE_COUNT, artifact.fileCount.toString())
            setProperty(KEY_TOTAL_SIZE_BYTES, artifact.totalSizeBytes.toString())
        }
        metadataFile.outputStream().use { properties.store(it, null) }
    }

    override fun load(): ImportedFileInfo? {
        if (!metadataFile.exists()) return null

        val properties = Properties().apply {
            metadataFile.inputStream().use(::load)
        }

        val filePath = properties.getProperty(KEY_FILE_PATH) ?: return null
        val format = properties.getProperty(KEY_FORMAT)?.let(ImportedFileFormat::valueOf) ?: return null
        val sizeBytes = properties.getProperty(KEY_SIZE_BYTES)?.toLongOrNull() ?: return null
        val receivedAt = properties.getProperty(KEY_RECEIVED_AT) ?: return null
        val sourceChannel = properties.getProperty(KEY_SOURCE_CHANNEL)
            ?.let { runCatching { ImportedSourceChannel.valueOf(it) }.getOrNull() }
            ?: ImportedSourceChannel.BLUETOOTH_STREAM
        val fileCount = properties.getProperty(KEY_FILE_COUNT)?.toIntOrNull() ?: 1
        val totalSizeBytes = properties.getProperty(KEY_TOTAL_SIZE_BYTES)?.toLongOrNull() ?: sizeBytes

        return ImportedFileInfo(
            file = File(filePath),
            sizeBytes = sizeBytes,
            format = format,
            receivedAt = receivedAt,
            sourceChannel = sourceChannel,
            fileCount = fileCount,
            totalSizeBytes = totalSizeBytes,
        )
    }

    override fun preserveLastSuccessfulOnFailure() {
        // No-op by design: failed imports must not overwrite the last successful artifact.
    }

    override fun onCurrentSessionCleared() {
        // No-op by design: imported artifacts persist independently from the current live session.
    }

    companion object {
        private const val METADATA_FILE_NAME = "last-import.properties"
        private const val KEY_FILE_PATH = "filePath"
        private const val KEY_SIZE_BYTES = "sizeBytes"
        private const val KEY_FORMAT = "format"
        private const val KEY_RECEIVED_AT = "receivedAt"
        private const val KEY_SOURCE_CHANNEL = "sourceChannel"
        private const val KEY_FILE_COUNT = "fileCount"
        private const val KEY_TOTAL_SIZE_BYTES = "totalSizeBytes"
    }
}
