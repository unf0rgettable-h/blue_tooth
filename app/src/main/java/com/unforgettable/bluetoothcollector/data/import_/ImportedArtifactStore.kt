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

        return ImportedFileInfo(
            file = File(filePath),
            sizeBytes = sizeBytes,
            format = format,
            receivedAt = receivedAt,
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
    }
}
