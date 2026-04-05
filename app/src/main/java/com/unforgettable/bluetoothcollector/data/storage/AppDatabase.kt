package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy

@Database(
    entities = [SessionEntity::class, MeasurementRecordEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(CollectorTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun measurementRecordDao(): MeasurementRecordDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                listOf(
                    "ALTER TABLE measurement_records ADD COLUMN protocolType TEXT DEFAULT NULL",
                    "ALTER TABLE measurement_records ADD COLUMN hzAngleRad REAL DEFAULT NULL",
                    "ALTER TABLE measurement_records ADD COLUMN vAngleRad REAL DEFAULT NULL",
                    "ALTER TABLE measurement_records ADD COLUMN slopeDistanceM REAL DEFAULT NULL",
                    "ALTER TABLE measurement_records ADD COLUMN coordinateE REAL DEFAULT NULL",
                    "ALTER TABLE measurement_records ADD COLUMN coordinateN REAL DEFAULT NULL",
                    "ALTER TABLE measurement_records ADD COLUMN coordinateH REAL DEFAULT NULL",
                ).forEach(database::execSQL)
            }
        }
    }
}

class CollectorTypeConverters {
    @TypeConverter
    fun fromDelimiterStrategy(value: DelimiterStrategy): String = value.name

    @TypeConverter
    fun toDelimiterStrategy(value: String): DelimiterStrategy = DelimiterStrategy.valueOf(value)
}
