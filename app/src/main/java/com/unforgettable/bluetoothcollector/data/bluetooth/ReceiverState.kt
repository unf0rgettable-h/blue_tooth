package com.unforgettable.bluetoothcollector.data.bluetooth

/**
 * Observable state of the experimental Bluetooth RFCOMM receiver (server socket).
 */
sealed interface ReceiverState {
    /** Receiver is idle — server socket not open. */
    data object Idle : ReceiverState

    /** Requesting Bluetooth discoverability from the user. */
    data object RequestingDiscoverability : ReceiverState

    /** Server socket is open, waiting for the instrument to connect. */
    data object Listening : ReceiverState

    /** Instrument has connected — reading incoming bytes. */
    data class Receiving(val bytesReceived: Long = 0) : ReceiverState

    /** Transfer completed successfully. */
    data class Completed(val bytesReceived: Long, val fileName: String) : ReceiverState

    /** Receiver stopped due to error. */
    data class Failed(val reason: String) : ReceiverState

    /** Receiver was cancelled by the user. */
    data object Cancelled : ReceiverState
}
