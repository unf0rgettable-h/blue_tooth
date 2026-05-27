package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectTransferArchiveWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun archive_project_directory_keeps_relative_paths() {
        val root = tempFolder.newFolder("ftp-root")
        File(root, "DATA").mkdirs()
        File(root, "DATA/job.dbx").writeText("dbx")
        File(root, "DATA/export.csv").writeText("csv")
        val output = tempFolder.newFile("project.zip")

        val result = ProjectTransferArchiveWriter().writeArchive(
            sourceDirectory = root,
            outputFile = output,
            receivedAt = "2026-05-27T21:00:00+08:00",
        )

        assertEquals(2, result.fileCount)
        ZipFile(output).use { zip ->
            assertTrue(zip.getEntry("DATA/job.dbx") != null)
            assertTrue(zip.getEntry("DATA/export.csv") != null)
        }
    }

    @Test
    fun archive_project_directory_skips_output_file_inside_source_directory() {
        val root = tempFolder.newFolder("ftp-root")
        File(root, "job.dbx").writeText("dbx")
        val output = File(root, "project.zip")

        ProjectTransferArchiveWriter().writeArchive(
            sourceDirectory = root,
            outputFile = output,
            receivedAt = "2026-05-27T21:00:00+08:00",
        )

        ZipFile(output).use { zip ->
            assertTrue(zip.getEntry("job.dbx") != null)
            assertFalse(zip.entries().asSequence().any { it.name == "project.zip" })
        }
    }
}
