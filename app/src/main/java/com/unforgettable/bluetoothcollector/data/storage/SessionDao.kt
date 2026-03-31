package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSession(): SessionEntity?

    @Query("UPDATE sessions SET isCurrent = 0 WHERE isCurrent = 1")
    suspend fun clearCurrentFlags()

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
