package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.withTransaction
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface TransactionRunner {
    suspend fun run(block: suspend () -> Unit)
}

class CollectorRepository(
    private val sessionDao: SessionDao,
    private val measurementRecordDao: MeasurementRecordDao,
    private val transactionRunner: TransactionRunner,
) {
    private val sessionMutex = Mutex()

    suspend fun ensureCurrentSession(
        startedAt: String,
        instrumentBrand: String,
        instrumentModel: String,
        bluetoothDeviceName: String,
        bluetoothDeviceAddress: String,
        delimiterStrategy: DelimiterStrategy,
    ): SessionEntity = sessionMutex.withLock {
        val existing = sessionDao.getCurrentSession()
        if (existing != null &&
            existing.instrumentBrand == instrumentBrand &&
            existing.instrumentModel == instrumentModel &&
            existing.bluetoothDeviceAddress == bluetoothDeviceAddress &&
            existing.delimiterStrategy == delimiterStrategy
        ) {
            return@withLock existing
        }
        if (existing != null) {
            throw IllegalStateException("current_session_metadata_mismatch_requires_clear")
        }

        val session = SessionEntity(
            sessionId = UUID.randomUUID().toString(),
            startedAt = startedAt,
            updatedAt = startedAt,
            instrumentBrand = instrumentBrand,
            instrumentModel = instrumentModel,
            bluetoothDeviceName = bluetoothDeviceName,
            bluetoothDeviceAddress = bluetoothDeviceAddress,
            delimiterStrategy = delimiterStrategy,
            isCurrent = true,
        )
        transactionRunner.run {
            sessionDao.clearCurrentFlags()
            sessionDao.upsert(session)
        }
        session
    }

    suspend fun appendRecord(
        sessionId: String,
        record: MeasurementRecordEntity,
    ) = sessionMutex.withLock {
        transactionRunner.run {
            val current = sessionDao.getCurrentSession()
                ?: throw IllegalStateException("current_session_missing")
            if (current.sessionId != sessionId) {
                throw IllegalStateException("record_append_requires_current_session")
            }
            measurementRecordDao.insert(
                record.copy(
                    sessionId = sessionId,
                    instrumentBrand = current.instrumentBrand,
                    instrumentModel = current.instrumentModel,
                    bluetoothDeviceName = current.bluetoothDeviceName,
                    bluetoothDeviceAddress = current.bluetoothDeviceAddress,
                ),
            )
            sessionDao.upsert(current.copy(updatedAt = record.receivedAt))
        }
    }

    suspend fun restoreCurrentSession(): SessionEntity? = sessionDao.getCurrentSession()

    suspend fun clearCurrentSession() = sessionMutex.withLock {
        val currentSession = sessionDao.getCurrentSession() ?: return@withLock
        transactionRunner.run {
            measurementRecordDao.deleteBySessionId(currentSession.sessionId)
            sessionDao.deleteBySessionId(currentSession.sessionId)
        }
    }

    companion object {
        fun fromDatabase(database: AppDatabase): CollectorRepository {
            return CollectorRepository(
                sessionDao = database.sessionDao(),
                measurementRecordDao = database.measurementRecordDao(),
                transactionRunner = TransactionRunner { block -> database.withTransaction { block() } },
            )
        }
    }
}
