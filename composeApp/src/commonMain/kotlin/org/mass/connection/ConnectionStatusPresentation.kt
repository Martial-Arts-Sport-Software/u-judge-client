package org.mass.connection

/** Localized status and optional reason shown for a connection lifecycle state. */
data class ConnectionStatusPresentation(
    val statusKey: String,
    val reasonKey: String? = null
)

fun connectionStatusPresentation(state: ConnectionState): ConnectionStatusPresentation? = when (state) {
    is ConnectionState.Reconnecting -> ConnectionStatusPresentation(
        statusKey = "connection_reconnecting",
        reasonKey = state.failure.localizationKey
    )
    else -> null
}
