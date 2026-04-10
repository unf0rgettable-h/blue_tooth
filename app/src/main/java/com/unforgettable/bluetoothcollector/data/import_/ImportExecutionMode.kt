package com.unforgettable.bluetoothcollector.data.import_

/**
 * How a batch-import session is executed once the transport is available.
 *
 * CLIENT_STREAM — the app reads bytes from an existing client-initiated connection.
 *                 Used by TS09/FlexLine after the app has connected to the instrument.
 *
 * RECEIVER_STREAM — the app reads bytes from an inbound connection accepted on a
 *                   server socket. Used by the experimental TS60 receiver mode.
 *
 * GUIDANCE_ONLY — no import execution occurs. The app shows guidance text instead.
 */
enum class ImportExecutionMode {
    CLIENT_STREAM,
    RECEIVER_STREAM,
    GUIDANCE_ONLY,
}
