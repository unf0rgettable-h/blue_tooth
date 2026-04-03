package com.unforgettable.bluetoothcollector.data.import_

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportProfileRegistryTest {

    @Test
    fun ts09_profile_is_supported_for_batch_import() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "TS09",
        )

        assertEquals(ImportProfileVerdict.SUPPORTED, profile.verdict)
        assertEquals("导入存储数据", profile.actionLabel)
    }

    @Test
    fun ts60_profile_defaults_to_guidance_only_until_supported_path_is_verified() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "TS60",
        )

        assertEquals(ImportProfileVerdict.GUIDANCE_ONLY, profile.verdict)
        assertEquals("查看导入说明", profile.actionLabel)
    }

    @Test
    fun unknown_model_falls_back_to_unsupported() {
        val profile = ImportProfileRegistry.resolve(
            brandId = "leica",
            modelId = "UNKNOWN_MODEL",
        )

        assertEquals(ImportProfileVerdict.UNSUPPORTED, profile.verdict)
        assertEquals("查看限制说明", profile.actionLabel)
    }
}
