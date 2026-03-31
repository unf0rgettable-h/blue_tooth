package com.unforgettable.bluetoothcollector.data.bluetooth

import com.unforgettable.bluetoothcollector.domain.model.DelimiterStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextStreamRecordParserTest {

    @Test
    fun line_delimited_parser_keeps_trailing_fragment_buffered() {
        val parser = TextStreamRecordParser()

        val result = parser.accept(
            chunk = "01 123.456\n02 234",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        assertEquals(1, result.completed.size)
        assertEquals("01 123.456", result.completed.first().rawPayload)
        assertEquals("02 234", result.remainingBuffer)
    }

    @Test
    fun whitespace_token_parser_splits_tokens() {
        val parser = TextStreamRecordParser()

        val result = parser.accept(
            chunk = "01 123.456 02 234.567",
            delimiterStrategy = DelimiterStrategy.WHITESPACE_TOKEN,
        )

        assertEquals(listOf("01", "123.456", "02", "234.567"), result.completed.map { it.rawPayload })
        assertEquals("", result.remainingBuffer)
    }

    @Test
    fun stop_receive_drops_incomplete_fragment() {
        val parser = TextStreamRecordParser()
        parser.accept(
            chunk = "01 123.456\n02 234",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        val flushed = parser.dropIncompleteFragment()

        assertEquals("02 234", flushed)
        assertEquals("", parser.remainingBuffer())
    }

    @Test
    fun overflow_trims_old_data_and_keeps_tail() {
        val parser = TextStreamRecordParser(bufferLimitBytes = 8, retainedTailBytes = 4)

        val result = parser.accept(
            chunk = "ABCDEFGHIJK",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        assertTrue(result.overflowed)
        assertEquals("HIJK", result.remainingBuffer)
    }

    @Test
    fun cleaned_text_is_preserved_when_not_parsed() {
        val parser = TextStreamRecordParser()

        val result = parser.accept(
            chunk = "RAW-RECORD\n",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        val record = result.completed.single()
        assertEquals("RAW-RECORD", record.rawPayload)
        assertEquals(null, record.parsedCode)
        assertEquals(null, record.parsedValue)
    }

    @Test
    fun control_character_garbage_is_dropped() {
        val parser = TextStreamRecordParser()

        val result = parser.accept(
            chunk = "\u0001\u0002\u0003\n",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        assertTrue(result.completed.isEmpty())
        assertEquals("", result.remainingBuffer)
    }

    @Test
    fun parser_extracts_code_and_value_when_shape_matches() {
        val parser = TextStreamRecordParser()

        val result = parser.accept(
            chunk = "01123.456\n",
            delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
        )

        val record = result.completed.single()
        assertEquals("01123.456", record.rawPayload)
        assertEquals("01", record.parsedCode)
        assertEquals("123.456", record.parsedValue)
    }
}
