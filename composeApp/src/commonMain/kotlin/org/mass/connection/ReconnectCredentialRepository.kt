package org.mass.connection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Platform boundary for the server-issued reconnect secret. */
interface ReconnectCredentialStorage {
    fun load(): String?

    fun save(credential: String)

    fun clear()

    fun loadPairingDeliveryProof(): String? = null

    fun savePairingDeliveryProof(proof: String) = Unit

    fun clearPairingDeliveryProof() = Unit

    fun loadPairedServer(): String? = null

    fun savePairedServer(server: String) = Unit

    fun clearPairedServer() = Unit
}

/**
 * The server this device is paired with: where to reconnect and which TLS key to require (server ADR-006). The SPKI pin,
 * not the address, identifies the server, so a changed DHCP address is updated by pairing again only if the key differs.
 */
data class PairedServer(
    val endpoint: String,
    val deviceId: String,
    val spkiSha256: ByteArray
) {
    fun toJson(): String = buildJsonObject {
        put("endpoint", endpoint)
        put("deviceId", deviceId)
        put("spkiSha256", spkiSha256.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') })
    }.toString()

    override fun equals(other: Any?): Boolean = other is PairedServer && endpoint == other.endpoint &&
        deviceId == other.deviceId && spkiSha256.contentEquals(other.spkiSha256)

    override fun hashCode(): Int = (endpoint.hashCode() * 31 + deviceId.hashCode()) * 31 + spkiSha256.contentHashCode()

    companion object {
        fun fromJson(json: String): PairedServer? = try {
            val body = Json.parseToJsonElement(json).jsonObject
            val hex = body.getValue("spkiSha256").jsonPrimitive.content
            require(hex.length == 64)
            PairedServer(
                endpoint = body.getValue("endpoint").jsonPrimitive.content,
                deviceId = body.getValue("deviceId").jsonPrimitive.content,
                spkiSha256 = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            )
        } catch (_: Exception) {
            null
        }
    }
}

class ReconnectCredentialRepository(private val storage: ReconnectCredentialStorage) {
    fun load(): String? = storage.load()

    fun save(credential: String) {
        storage.save(credential)
    }

    fun clear() {
        storage.clear()
    }

    fun loadPairingDeliveryProof(): String? = storage.loadPairingDeliveryProof()

    fun savePairingDeliveryProof(proof: String) {
        storage.savePairingDeliveryProof(proof)
    }

    fun clearPairingDeliveryProof() {
        storage.clearPairingDeliveryProof()
    }

    fun loadPairedServer(): PairedServer? = storage.loadPairedServer()?.let(PairedServer::fromJson)

    fun savePairedServer(server: PairedServer) {
        storage.savePairedServer(server.toJson())
    }

    /** Removes everything that lets this device reconnect without a new operator approval. */
    fun forget() {
        storage.clear()
        storage.clearPairedServer()
        storage.clearPairingDeliveryProof()
    }
}
