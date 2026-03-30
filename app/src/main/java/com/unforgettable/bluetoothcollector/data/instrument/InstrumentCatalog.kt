package com.unforgettable.bluetoothcollector.data.instrument

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import com.unforgettable.bluetoothcollector.domain.model.InstrumentBrand
import com.unforgettable.bluetoothcollector.domain.model.InstrumentModel

object InstrumentCatalog {
    const val CLASSIC_BLUETOOTH_SPP: String = InstrumentModel.CLASSIC_BLUETOOTH_SPP

    val brands: List<InstrumentBrand> = listOf(
        InstrumentBrand(id = "leica", displayName = "徕卡(Leica Geosystems)"),
        InstrumentBrand(id = "sokkia", displayName = "索佳(Sokkia)"),
        InstrumentBrand(id = "topcon", displayName = "拓普康(Topcon)"),
        InstrumentBrand(id = "south", displayName = "南方测绘(South Surveying)"),
        InstrumentBrand(id = "chc", displayName = "华测(CHC Navigation)"),
        InstrumentBrand(id = "hi_target", displayName = "中海达(Hi-Target)"),
    )

    val models: List<InstrumentModel> = listOf(
        InstrumentModel(id = "TS02", brandId = "leica", displayName = "TS02", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),
        InstrumentModel(id = "TS06", brandId = "leica", displayName = "TS06", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),
        InstrumentModel(id = "TS09", brandId = "leica", displayName = "TS09", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),
        InstrumentModel(id = "TS16", brandId = "leica", displayName = "TS16", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),
        InstrumentModel(id = "TS50", brandId = "leica", displayName = "TS50", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),
        InstrumentModel(id = "TS60", brandId = "leica", displayName = "TS60", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),
        InstrumentModel(id = "iCON_iCR80", brandId = "leica", displayName = "iCON iCR80", delimiterStrategy = DelimiterStrategy.LINE_DELIMITED),

        InstrumentModel(id = "SX-103", brandId = "sokkia", displayName = "SX-103", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "SX-105", brandId = "sokkia", displayName = "SX-105", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "SX-113", brandId = "sokkia", displayName = "SX-113", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "CX-101", brandId = "sokkia", displayName = "CX-101", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "CX-105", brandId = "sokkia", displayName = "CX-105", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "iM-52", brandId = "sokkia", displayName = "iM-52", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "iM-105", brandId = "sokkia", displayName = "iM-105", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "iX-1003", brandId = "sokkia", displayName = "iX-1003", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),

        InstrumentModel(id = "ES-105", brandId = "topcon", displayName = "ES-105", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "ES-120", brandId = "topcon", displayName = "ES-120", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "MS-05AX", brandId = "topcon", displayName = "MS-05AX", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "MS1AX", brandId = "topcon", displayName = "MS1AX", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "GT-1200", brandId = "topcon", displayName = "GT-1200", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),

        InstrumentModel(id = "NTS-362R", brandId = "south", displayName = "NTS-362R", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "552R", brandId = "south", displayName = "552R", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "NTS-662R", brandId = "south", displayName = "NTS-662R", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "R1-062", brandId = "south", displayName = "R1-062", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),

        InstrumentModel(id = "HTS-221", brandId = "chc", displayName = "HTS-221", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "HTS-321", brandId = "chc", displayName = "HTS-321", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "HTS-661", brandId = "chc", displayName = "HTS-661", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),

        InstrumentModel(id = "ZTS-121", brandId = "hi_target", displayName = "ZTS-121", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "ZTS-221", brandId = "hi_target", displayName = "ZTS-221", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "iTrack-5", brandId = "hi_target", displayName = "iTrack-5", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
        InstrumentModel(id = "iAngle_X3", brandId = "hi_target", displayName = "iAngle X3", delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN),
    )
}
