package com.unforgettable.bluetoothcollector.data.bluetooth

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy

data class ParsedIncomingRecord(
    val rawPayload: String,
    val parsedCode: String?,
    val parsedValue: String?,
)

data class TextStreamParseResult(
    val completed: List<ParsedIncomingRecord>,
    val remainingBuffer: String,
    val overflowed: Boolean,
)

class TextStreamRecordParser(
    private val bufferLimitBytes: Int = DEFAULT_BUFFER_LIMIT_BYTES,
    private val retainedTailBytes: Int = DEFAULT_RETAINED_TAIL_BYTES,
) {
    private var buffer: String = ""

    fun accept(
        chunk: String,
        delimiterStrategy: DelimiterStrategy,
    ): TextStreamParseResult {
        buffer += chunk
        var overflowed = false

        if (buffer.length > bufferLimitBytes) {
            buffer = buffer.takeLast(retainedTailBytes.coerceAtMost(buffer.length))
            overflowed = true
        }

        val completed = when (delimiterStrategy) {
            DelimiterStrategy.LINE_DELIMITED -> consumeLineDelimited()
            DelimiterStrategy.WHITESPACE_TOKEN -> consumeWhitespaceTokens()
        }

        return TextStreamParseResult(
            completed = completed,
            remainingBuffer = buffer,
            overflowed = overflowed,
        )
    }

    fun dropIncompleteFragment(): String {
        val dropped = buffer
        buffer = ""
        return dropped
    }

    fun remainingBuffer(): String = buffer

    private fun consumeLineDelimited(): List<ParsedIncomingRecord> {
        val segments = buffer.split(Regex("\\r\\n|\\n|\\r"))
        val trailingHasDelimiter = buffer.endsWith("\n") || buffer.endsWith("\r")
        val completedSegments = if (trailingHasDelimiter) segments else segments.dropLast(1)
        buffer = if (trailingHasDelimiter) "" else segments.lastOrNull().orEmpty()
        return completedSegments.mapNotNull(::toRecordOrNull)
    }

    private fun consumeWhitespaceTokens(): List<ParsedIncomingRecord> {
        val tokens = buffer.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .mapNotNull(::toRecordOrNull)
        buffer = ""
        return tokens
    }

    private fun toRecordOrNull(input: String): ParsedIncomingRecord? {
        val cleaned = input.filter { character ->
            character >= ' ' && character.code != 127
        }.trim()

        if (cleaned.isBlank()) return null

        val parsedCode = cleaned
            .takeIf { it.length > 2 && it[0].isDigit() && it[1].isDigit() }
            ?.take(2)
        val parsedValue = parsedCode?.let { cleaned.drop(2).takeIf(String::isNotBlank) }

        return ParsedIncomingRecord(
            rawPayload = cleaned,
            parsedCode = parsedCode,
            parsedValue = parsedValue,
        )
    }

    companion object {
        private const val DEFAULT_BUFFER_LIMIT_BYTES = 32 * 1024
        private const val DEFAULT_RETAINED_TAIL_BYTES = 8 * 1024
    }
}
