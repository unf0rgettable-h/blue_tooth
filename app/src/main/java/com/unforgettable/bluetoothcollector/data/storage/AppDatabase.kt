package com.unforgettable.bluetoothcollector.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy

@Database(
    entities = [SessionEntity::class, MeasurementRecordEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(CollectorTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun measurementRecordDao(): MeasurementRecordDao
}

class CollectorTypeConverters {
    @TypeConverter
    fun fromDelimiterStrategy(value: DelimiterStrategy): String = value.name

    @TypeConverter
    fun toDelimiterStrategy(value: String): DelimiterStrategy = DelimiterStrategy.valueOf(value)
}
