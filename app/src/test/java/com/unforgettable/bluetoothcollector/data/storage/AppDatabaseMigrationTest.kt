package com.unforgettable.bluetoothcollector.data.storage

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @Test
    fun migration_1_2_preserves_existing_records_and_adds_geocom_columns() {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "migration-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        val factory = FrameworkSQLiteOpenHelperFactory()
        val createHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createVersion1Schema(db)
                            insertVersion1Fixture(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        )
        createHelper.writableDatabase.close()
        createHelper.close()

        val upgradeHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            AppDatabase.MIGRATION_1_2.migrate(db)
                        }
                    },
                ).build(),
        )

        val migrated = upgradeHelper.writableDatabase

        migrated.query(
            "SELECT rawPayload, parsedCode, protocolType, hzAngleRad, coordinateH " +
                "FROM measurement_records WHERE id = 'r1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("01123.456", cursor.getString(0))
            assertEquals("01", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
        }

        migrated.query("PRAGMA table_info(measurement_records)").use { cursor ->
            val columnNames = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                columnNames += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }

            assertTrue(columnNames.contains("protocolType"))
            assertTrue(columnNames.contains("hzAngleRad"))
            assertTrue(columnNames.contains("vAngleRad"))
            assertTrue(columnNames.contains("slopeDistanceM"))
            assertTrue(columnNames.contains("coordinateE"))
            assertTrue(columnNames.contains("coordinateN"))
            assertTrue(columnNames.contains("coordinateH"))
        }

        migrated.close()
        upgradeHelper.close()
        context.deleteDatabase(databaseName)
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sessions (
                sessionId TEXT NOT NULL PRIMARY KEY,
                startedAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                instrumentBrand TEXT NOT NULL,
                instrumentModel TEXT NOT NULL,
                bluetoothDeviceName TEXT NOT NULL,
                bluetoothDeviceAddress TEXT NOT NULL,
                delimiterStrategy TEXT NOT NULL,
                isCurrent INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS measurement_records (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                sequence INTEGER NOT NULL,
                receivedAt TEXT NOT NULL,
                instrumentBrand TEXT NOT NULL,
                instrumentModel TEXT NOT NULL,
                bluetoothDeviceName TEXT NOT NULL,
                bluetoothDeviceAddress TEXT NOT NULL,
                rawPayload TEXT NOT NULL,
                parsedCode TEXT,
                parsedValue TEXT,
                FOREIGN KEY(sessionId) REFERENCES sessions(sessionId) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_measurement_records_sessionId " +
                "ON measurement_records(sessionId)",
        )
    }

    private fun insertVersion1Fixture(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO sessions(
                sessionId, startedAt, updatedAt, instrumentBrand, instrumentModel,
                bluetoothDeviceName, bluetoothDeviceAddress, delimiterStrategy, isCurrent
            ) VALUES (
                's1', '2026-03-31T10:00:00+08:00', '2026-03-31T10:00:00+08:00',
                'leica', 'TS02', 'Leica TS02', '00:11:22:33:44:55', 'LINE_DELIMITED', 1
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO measurement_records(
                id, sessionId, sequence, receivedAt, instrumentBrand, instrumentModel,
                bluetoothDeviceName, bluetoothDeviceAddress, rawPayload, parsedCode, parsedValue
            ) VALUES (
                'r1', 's1', 1, '2026-03-31T10:00:01+08:00', 'leica', 'TS02',
                'Leica TS02', '00:11:22:33:44:55', '01123.456', '01', '123.456'
            )
            """.trimIndent(),
        )
    }
}
