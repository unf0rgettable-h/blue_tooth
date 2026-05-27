package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportedArtifactStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun imported_artifact_survives_reload() {
        val root = tempFolder.newFolder("artifact-store")
        val store = ImportedArtifactStore(root)
        val artifact = sampleArtifact(root)

        store.save(artifact)

        val reloaded = ImportedArtifactStore(root).load()
        assertNotNull(reloaded)
        assertEquals(artifact.file.absolutePath, reloaded?.file?.absolutePath)
        assertEquals(artifact.format, reloaded?.format)
        assertEquals(artifact.sourceChannel, reloaded?.sourceChannel)
        assertEquals(artifact.fileCount, reloaded?.fileCount)
        assertEquals(artifact.totalSizeBytes, reloaded?.totalSizeBytes)
    }

    @Test
    fun ftp_project_artifact_metadata_survives_reload() {
        val root = tempFolder.newFolder("artifact-store")
        val store = ImportedArtifactStore(root)
        val zipFile = File(root, "ts60-project.zip").apply {
            writeText("zip bytes")
        }
        val artifact = ImportedFileInfo(
            file = zipFile,
            sizeBytes = zipFile.length(),
            format = ImportedFileFormat.ZIP,
            receivedAt = "2026-05-27T21:00:00+08:00",
            sourceChannel = ImportedSourceChannel.FTP_WLAN_PROJECT,
            fileCount = 3,
            totalSizeBytes = 12_345L,
        )

        store.save(artifact)

        val reloaded = ImportedArtifactStore(root).load()
        assertNotNull(reloaded)
        assertEquals(ImportedSourceChannel.FTP_WLAN_PROJECT, reloaded?.sourceChannel)
        assertEquals(3, reloaded?.fileCount)
        assertEquals(12_345L, reloaded?.totalSizeBytes)
    }

    @Test
    fun failed_import_does_not_replace_last_successful_artifact() {
        val root = tempFolder.newFolder("artifact-store")
        val store = ImportedArtifactStore(root)
        val successful = sampleArtifact(root, "import-ok.gsi")

        store.save(successful)
        store.preserveLastSuccessfulOnFailure()

        val reloaded = store.load()
        assertNotNull(reloaded)
        assertEquals(successful.file.absolutePath, reloaded?.file?.absolutePath)
    }

    @Test
    fun clear_current_session_does_not_delete_imported_artifact() {
        val root = tempFolder.newFolder("artifact-store")
        val store = ImportedArtifactStore(root)
        val artifact = sampleArtifact(root, "import-still-there.gsi")

        store.save(artifact)
        store.onCurrentSessionCleared()

        val reloaded = store.load()
        assertNotNull(reloaded)
        assertEquals(artifact.file.absolutePath, reloaded?.file?.absolutePath)
    }

    @Test
    fun empty_store_returns_null() {
        val root = tempFolder.newFolder("artifact-store")
        val store = ImportedArtifactStore(root)

        assertNull(store.load())
    }

    private fun sampleArtifact(root: File, name: String = "import-sample.gsi"): ImportedFileInfo {
        val file = File(root, name).apply {
            writeText("*110001+000000000")
        }
        return ImportedFileInfo(
            file = file,
            sizeBytes = file.length(),
            format = ImportedFileFormat.GSI,
            receivedAt = "2026-04-03T21:00:00+08:00",
        )
    }
}
