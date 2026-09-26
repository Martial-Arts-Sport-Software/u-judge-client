package org.mass.connection

import io.ktor.client.HttpClient

/** HTTPS/WSS client that trusts only the key accepted by [trust] and supports the realtime WebSocket. */
expect fun createHttpClient(trust: ServerTrust): HttpClient
