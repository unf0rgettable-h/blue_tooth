package com.unforgettable.bluetoothcollector.data.bluetooth

/**
 * How the app establishes a Bluetooth transport to the instrument.
 *
 * CLIENT — the app initiates an outbound RFCOMM connection to a bonded device.
 *          This is the verified TS09/FlexLine path.
 *
 * RECEIVER — the app opens an RFCOMM server socket and waits for the instrument
 *            to connect inbound. This is the experimental TS60 path.
 */
enum class TransportConnectionMode {
    CLIENT,
    RECEIVER,
}
