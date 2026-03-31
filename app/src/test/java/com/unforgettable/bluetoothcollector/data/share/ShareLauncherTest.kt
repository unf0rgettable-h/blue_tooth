package com.unforgettable.bluetoothcollector.data.share

import android.content.Intent
import android.net.Uri
import com.unforgettable.bluetoothcollector.data.export.TxtExportWriter
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.ExportFormat
import com.unforgettable.bluetoothcollector.domain.model.MeasurementRecord
import com.unforgettable.bluetoothcollector.domain.model.Session
import java.io.File
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShareLauncherTest {

    @Test
    fun share_launcher_starts_action_send_with_file_provider_uri_for_exported_txt_file() {
        val application = RuntimeEnvironment.getApplication()
        val launcher = ShareLauncher()
        val exportedFile = TxtExportWriter().write(
            directory = File(application.filesDir, "exports"),
            session = sampleSession(),
            records = listOf(sampleRecord(sequence = 1, rawPayload = "FIRST")),
            exportedAt = OffsetDateTime.parse("2026-03-31T10:15:00+08:00"),
        )

        launcher.share(
            context = application,
            exportedFile = exportedFile,
            format = ExportFormat.TXT,
        )

        assertTrue(exportedFile.exists())
        assertEquals(File(application.filesDir, "exports").absolutePath, exportedFile.parentFile?.absolutePath)

        val chooserIntent = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)

        val sendIntent = chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, sendIntent?.action)
        assertEquals("text/plain", sendIntent?.type)

        val streamUri = sendIntent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertEquals("content", streamUri?.scheme)
        assertTrue(streamUri.toString().startsWith("content://${application.packageName}.fileprovider/"))
        assertTrue((sendIntent!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    private fun sampleSession(): Session {
        return Session(
            sessionId = "session-1",
            startedAt = "2026-03-31T10:00:00+08:00",
            updatedAt = "2026-03-31T10:01:00+08:00",
            instrumentBrand = "徕卡",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
            isCurrent = true,
        )
    }

    private fun sampleRecord(sequence: Long, rawPayload: String): MeasurementRecord {
        return MeasurementRecord(
            id = "row-$sequence",
            sequence = sequence,
            receivedAt = "2026-03-31T10:00:0${sequence}+08:00",
            instrumentBrand = "徕卡",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            rawPayload = rawPayload,
            parsedCode = null,
            parsedValue = null,
        )
    }
}
