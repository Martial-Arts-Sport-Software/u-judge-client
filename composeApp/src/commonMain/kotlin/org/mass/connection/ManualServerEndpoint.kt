package org.mass.connection

import io.ktor.http.Url

sealed interface ManualServerEndpointResult {
    data class Valid(val endpoint: Url) : ManualServerEndpointResult
    data object Invalid : ManualServerEndpointResult
}

fun manualServerEndpoint(host: String, port: String): ManualServerEndpointResult {
    val normalizedHost = host.trim().removeSurrounding("[", "]")
    val normalizedPort = port.trim().toIntOrNull() ?: return ManualServerEndpointResult.Invalid
    if (
        normalizedHost.isBlank() ||
        normalizedHost.any(Char::isWhitespace) ||
        "://" in normalizedHost ||
        normalizedHost.any { it in "/?#" } ||
        normalizedPort !in 1..65535
    ) {
        return ManualServerEndpointResult.Invalid
    }

    return try {
        val address = if (':' in normalizedHost) "[$normalizedHost]" else normalizedHost
        ManualServerEndpointResult.Valid(metadataEndpoint(address, normalizedPort))
    } catch (_: IllegalArgumentException) {
        ManualServerEndpointResult.Invalid
    }
}
