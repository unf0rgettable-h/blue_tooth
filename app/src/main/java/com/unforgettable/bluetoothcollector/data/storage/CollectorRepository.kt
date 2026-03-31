package com.unforgettable.bluetoothcollector.data.storage

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import java.util.UUID

class CollectorRepository(
    private val sessionDao: SessionDao,
    private val measurementRecordDao: MeasurementRecordDao,
) {
    suspend fun ensureCurrentSession(
        startedAt: String,
        instrumentBrand: String,
        instrumentModel: String,
        bluetoothDeviceName: String,
        bluetoothDeviceAddress: String,
        delimiterStrategy: DelimiterStrategy,
    ): SessionEntity {
        val existing = sessionDao.getCurrentSession()
        if (existing != null &&
            existing.instrumentBrand == instrumentBrand &&
            existing.instrumentModel == instrumentModel &&
            existing.bluetoothDeviceAddress == bluetoothDeviceAddress
        ) {
            return existing
        }

        val session = SessionEntity(
            sessionId = existing?.sessionId ?: UUID.randomUUID().toString(),
            startedAt = existing?.startedAt ?: startedAt,
            updatedAt = startedAt,
            instrumentBrand = instrumentBrand,
            instrumentModel = instrumentModel,
            bluetoothDeviceName = bluetoothDeviceName,
            bluetoothDeviceAddress = bluetoothDeviceAddress,
            delimiterStrategy = delimiterStrategy,
            isCurrent = true,
        )
        sessionDao.upsert(session)
        return session
    }

    suspend fun appendRecord(
        sessionId: String,
        record: MeasurementRecordEntity,
    ) {
        measurementRecordDao.insert(record.copy(sessionId = sessionId))
    }

    suspend fun restoreCurrentSession(): SessionEntity? = sessionDao.getCurrentSession()

    suspend fun clearCurrentSession() {
        val currentSession = sessionDao.getCurrentSession() ?: return
        measurementRecordDao.deleteBySessionId(currentSession.sessionId)
        sessionDao.clearCurrentSession()
    }
}
