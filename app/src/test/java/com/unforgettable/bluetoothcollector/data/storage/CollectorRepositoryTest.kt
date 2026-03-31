package com.unforgettable.bluetoothcollector.data.storage

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorRepositoryTest {
    private val noOpTransactionRunner = TransactionRunner { block -> block() }

    @Test
    fun creates_current_session_when_absent() = runBlocking {
        val sessionDao = FakeSessionDao()
        val recordDao = FakeMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)

        val session = repository.ensureCurrentSession(
            startedAt = "2026-03-31T10:00:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        assertTrue(session.isCurrent)
        assertEquals("leica", session.instrumentBrand)
        assertEquals(1, sessionDao.sessions.size)
    }

    @Test
    fun appends_record_to_current_session() = runBlocking {
        val sessionDao = FakeSessionDao()
        val recordDao = FakeMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)
        val session = repository.ensureCurrentSession(
            startedAt = "2026-03-31T10:00:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        repository.appendRecord(
            sessionId = session.sessionId,
            record = MeasurementRecordEntity(
                id = "r1",
                sessionId = session.sessionId,
                sequence = 1,
                receivedAt = "2026-03-31T10:00:01+08:00",
                instrumentBrand = "leica",
                instrumentModel = "TS02",
                bluetoothDeviceName = "Leica TS02",
                bluetoothDeviceAddress = "00:11:22:33:44:55",
                rawPayload = "01123.456",
                parsedCode = "01",
                parsedValue = "123.456",
            ),
        )

        assertEquals(1, recordDao.records.size)
        assertEquals("r1", recordDao.records.single().id)
    }

    @Test
    fun restores_current_session_after_restart() = runBlocking {
        val sessionDao = FakeSessionDao().apply {
            sessions += SessionEntity(
                sessionId = "s1",
                startedAt = "2026-03-31T10:00:00+08:00",
                updatedAt = "2026-03-31T10:00:05+08:00",
                instrumentBrand = "leica",
                instrumentModel = "TS02",
                bluetoothDeviceName = "Leica TS02",
                bluetoothDeviceAddress = "00:11:22:33:44:55",
                delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
                isCurrent = true,
            )
        }
        val recordDao = FakeMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)

        val restored = repository.restoreCurrentSession()

        assertEquals("s1", restored?.sessionId)
    }

    @Test
    fun clear_current_session_deletes_session_records() = runBlocking {
        val sessionDao = FakeSessionDao().apply {
            sessions += SessionEntity(
                sessionId = "s1",
                startedAt = "2026-03-31T10:00:00+08:00",
                updatedAt = "2026-03-31T10:00:05+08:00",
                instrumentBrand = "leica",
                instrumentModel = "TS02",
                bluetoothDeviceName = "Leica TS02",
                bluetoothDeviceAddress = "00:11:22:33:44:55",
                delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
                isCurrent = true,
            )
        }
        val recordDao = FakeMeasurementRecordDao().apply {
            records += MeasurementRecordEntity(
                id = "r1",
                sessionId = "s1",
                sequence = 1,
                receivedAt = "2026-03-31T10:00:01+08:00",
                instrumentBrand = "leica",
                instrumentModel = "TS02",
                bluetoothDeviceName = "Leica TS02",
                bluetoothDeviceAddress = "00:11:22:33:44:55",
                rawPayload = "01123.456",
                parsedCode = "01",
                parsedValue = "123.456",
            )
        }
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)

        repository.clearCurrentSession()

        assertTrue(sessionDao.sessions.isEmpty())
        assertTrue(recordDao.records.isEmpty())
        assertNull(repository.restoreCurrentSession())
    }

    @Test
    fun same_session_continuation_allowed_for_same_instrument_and_device() = runBlocking {
        val sessionDao = FakeSessionDao()
        val recordDao = FakeMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)

        val first = repository.ensureCurrentSession(
            startedAt = "2026-03-31T10:00:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )
        val second = repository.ensureCurrentSession(
            startedAt = "2026-03-31T10:05:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        assertEquals(first.sessionId, second.sessionId)
        assertEquals(1, sessionDao.sessions.size)
    }

    @Test
    fun metadata_change_requires_current_session_clear() = runBlocking {
        val sessionDao = FakeSessionDao()
        val recordDao = FakeMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)

        repository.ensureCurrentSession(
            startedAt = "2026-03-31T10:00:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS02",
            bluetoothDeviceName = "Leica TS02",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        val failure = runCatching {
            repository.ensureCurrentSession(
                startedAt = "2026-03-31T10:05:00+08:00",
                instrumentBrand = "sokkia",
                instrumentModel = "SX-103",
                bluetoothDeviceName = "Sokkia SX-103",
                bluetoothDeviceAddress = "66:77:88:99:AA:BB",
                delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN,
            )
        }

        assertTrue(failure.isFailure)
        assertEquals(
            "current_session_metadata_mismatch_requires_clear",
            failure.exceptionOrNull()?.message,
        )
    }

    @Test
    fun repository_stays_persistence_focused() = runBlocking {
        val sessionDao = FakeSessionDao()
        val recordDao = FakeMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)
        val methodNames = repository.javaClass.declaredMethods.map { it.name }

        assertFalse(methodNames.any { it.contains("connect", ignoreCase = true) })
        assertFalse(methodNames.any { it.contains("discover", ignoreCase = true) })
    }
}

private class FakeSessionDao : SessionDao {
    val sessions = mutableListOf<SessionEntity>()

    override suspend fun upsert(session: SessionEntity) {
        sessions.removeAll { it.sessionId == session.sessionId }
        sessions += session
    }

    override suspend fun getCurrentSession(): SessionEntity? = sessions.firstOrNull { it.isCurrent }

    override suspend fun clearCurrentFlags() {
        sessions.replaceAll { it.copy(isCurrent = false) }
    }

    override suspend fun deleteBySessionId(sessionId: String) {
        sessions.removeAll { it.sessionId == sessionId }
    }
}

private class FakeMeasurementRecordDao : MeasurementRecordDao {
    val records = mutableListOf<MeasurementRecordEntity>()

    override suspend fun insert(record: MeasurementRecordEntity) {
        records += record
    }

    override suspend fun deleteBySessionId(sessionId: String) {
        records.removeAll { it.sessionId == sessionId }
    }
}
