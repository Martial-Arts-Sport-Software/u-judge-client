package org.mass.connection

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
