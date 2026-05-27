package com.unforgettable.bluetoothcollector.data.import_

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ProjectTransferArchiveResult(
    val file: File,
    val fileCount: Int,
    val totalSizeBytes: Long,
)

/**
 * TS60 WLAN/FTP 项目文件归档器。
 *
 * Captivate 可以上传一个 DBX 文件夹或多个导出文件；分享给外部应用时统一打成 ZIP，
 * 既保留目录结构，也避免多个文件分享时丢失项目上下文。
 */
class ProjectTransferArchiveWriter {

    /**
     * 将 sourceDirectory 内的所有普通文件写入 outputFile。
     *
     * outputFile 如果位于 sourceDirectory 内，会被显式跳过，避免 ZIP 包把自己再次写进去。
     */
    fun writeArchive(
        sourceDirectory: File,
        outputFile: File,
        receivedAt: String,
    ): ProjectTransferArchiveResult {
        sourceDirectory.mkdirs()
        outputFile.parentFile?.mkdirs()
        val root = sourceDirectory.canonicalFile
        val outputCanonical = outputFile.canonicalFile
        var fileCount = 0
        var totalSizeBytes = 0L

        ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
            root.walkTopDown()
                .filter(File::isFile)
                .filter { it.canonicalFile != outputCanonical }
                .forEach { file ->
                    val canonicalFile = file.canonicalFile
                    require(canonicalFile.path.startsWith(root.path)) {
                        "archive_file_outside_root"
                    }
                    val relativePath = root.toPath().relativize(canonicalFile.toPath()).toString()
                        .replace(File.separatorChar, '/')
                    zip.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                    fileCount += 1
                    totalSizeBytes += file.length()
                }
        }

        return ProjectTransferArchiveResult(
            file = outputFile,
            fileCount = fileCount,
            totalSizeBytes = totalSizeBytes,
        )
    }
}
