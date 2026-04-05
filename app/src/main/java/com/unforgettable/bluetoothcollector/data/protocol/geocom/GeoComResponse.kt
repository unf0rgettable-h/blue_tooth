package com.unforgettable.bluetoothcollector.data.protocol.geocom

data class GeoComResponse(
    val rawPayload: String,
    val communicationReturnCode: Int,
    val transactionId: Int,
    val returnCode: Int,
    val parameters: List<String>,
) {
    val isSuccessful: Boolean
        get() = communicationReturnCode == 0 && returnCode == 0

    companion object {
        private val RESPONSE_PATTERN =
            Regex("""%R1P,(-?\d+),(\d+):(-?\d+)(?:,(.*))?""")

        fun parse(raw: String): GeoComResponse? {
            val normalized = raw.trim()
            val match = RESPONSE_PATTERN.matchEntire(normalized) ?: return null
            val communicationReturnCode = match.groupValues[1].toIntOrNull() ?: return null
            if (communicationReturnCode != 0) return null

            val transactionId = match.groupValues[2].toIntOrNull() ?: return null
            val returnCode = match.groupValues[3].toIntOrNull() ?: return null
            val parameters = match.groupValues[4]
                .takeIf(String::isNotBlank)
                ?.split(",")
                ?: emptyList()

            return GeoComResponse(
                rawPayload = normalized,
                communicationReturnCode = communicationReturnCode,
                transactionId = transactionId,
                returnCode = returnCode,
                parameters = parameters,
            )
        }
    }
}
