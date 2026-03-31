package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MeasurementRecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: MeasurementRecordEntity)

    @Query("SELECT * FROM measurement_records WHERE sessionId = :sessionId ORDER BY sequence ASC")
    suspend fun getBySessionIdOrdered(sessionId: String): List<MeasurementRecordEntity>

    @Query("DELETE FROM measurement_records WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
