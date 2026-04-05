package com.unforgettable.bluetoothcollector.data.storage

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementRecordGeoComFieldsTest {
    private val noOpTransactionRunner = TransactionRunner { block -> block() }

    @Test
    fun appendRecord_preserves_geocom_fields() = runBlocking {
        val sessionDao = GeoComTestSessionDao()
        val recordDao = GeoComTestMeasurementRecordDao()
        val repository = CollectorRepository(sessionDao, recordDao, noOpTransactionRunner)
        val session = repository.ensureCurrentSession(
            startedAt = "2026-03-31T10:00:00+08:00",
            instrumentBrand = "leica",
            instrumentModel = "TS60",
            bluetoothDeviceName = "Leica TS60",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        repository.appendRecord(
            sessionId = session.sessionId,
            record = MeasurementRecordEntity(
                id = "g1",
                sessionId = session.sessionId,
                sequence = 1,
                receivedAt = "2026-03-31T10:00:01+08:00",
                instrumentBrand = "ignored",
                instrumentModel = "ignored",
                bluetoothDeviceName = "ignored",
                bluetoothDeviceAddress = "ignored",
                rawPayload = "%R1P,0,0:0,1.0,2.0,3.0",
                parsedCode = "GEOCOM",
                parsedValue = "1.0,2.0,3.0",
                protocolType = "GEOCOM",
                hzAngleRad = 1.0,
                vAngleRad = 2.0,
                slopeDistanceM = 3.0,
                coordinateE = 4.0,
                coordinateN = 5.0,
                coordinateH = 6.0,
            ),
        )

        val saved = recordDao.records.single()
        assertEquals("GEOCOM", saved.protocolType)
        assertEquals(1.0, saved.hzAngleRad ?: error("missing hzAngleRad"), 0.0)
        assertEquals(2.0, saved.vAngleRad ?: error("missing vAngleRad"), 0.0)
        assertEquals(3.0, saved.slopeDistanceM ?: error("missing slopeDistanceM"), 0.0)
        assertEquals(4.0, saved.coordinateE ?: error("missing coordinateE"), 0.0)
        assertEquals(5.0, saved.coordinateN ?: error("missing coordinateN"), 0.0)
        assertEquals(6.0, saved.coordinateH ?: error("missing coordinateH"), 0.0)
    }
}

private class GeoComTestSessionDao : SessionDao {
    private val sessions = mutableListOf<SessionEntity>()

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

private class GeoComTestMeasurementRecordDao : MeasurementRecordDao {
    val records = mutableListOf<MeasurementRecordEntity>()

    override suspend fun insert(record: MeasurementRecordEntity) {
        records += record
    }

    override suspend fun getBySessionIdOrdered(sessionId: String): List<MeasurementRecordEntity> {
        return records.filter { it.sessionId == sessionId }.sortedBy { it.sequence }
    }

    override suspend fun deleteBySessionId(sessionId: String) {
        records.removeAll { it.sessionId == sessionId }
    }
}
