package com.unforgettable.bluetoothcollector.data.instrument

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentCatalogTest {

    @Test
    fun every_model_uses_known_brand_delimiter_and_transport() {
        val knownBrandIds = InstrumentCatalog.brands.map { it.id }.toSet()

        assertFalse("Catalog should expose at least one brand", knownBrandIds.isEmpty())
        assertFalse("Catalog should expose at least one model", InstrumentCatalog.models.isEmpty())

        InstrumentCatalog.models.forEach { model ->
            assertTrue(
                "Unknown brandId '${model.brandId}' for model '${model.modelId}'",
                model.brandId in knownBrandIds,
            )
            assertNotNull(
                "Missing delimiter strategy for model '${model.modelId}'",
                model.delimiterStrategy,
            )
            assertEquals(
                "Unexpected transport for model '${model.modelId}'",
                InstrumentCatalog.CLASSIC_BLUETOOTH_SPP,
                model.expectedTransport,
            )
        }
    }
}
