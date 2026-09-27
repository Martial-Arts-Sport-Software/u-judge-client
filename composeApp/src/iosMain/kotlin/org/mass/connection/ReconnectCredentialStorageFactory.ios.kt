@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package org.mass.connection

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

actual fun createReconnectCredentialStorage(context: Any?): ReconnectCredentialStorage =
    IosReconnectCredentialStorage()

private class IosReconnectCredentialStorage : ReconnectCredentialStorage {
    override fun load(): String? = load(ACCOUNT_NAME)

    override fun save(credential: String) = save(ACCOUNT_NAME, credential)

    override fun clear() = clear(ACCOUNT_NAME)

    override fun loadPairingDeliveryProof(): String? = load(PAIRING_PROOF_ACCOUNT)

    override fun savePairingDeliveryProof(proof: String) = save(PAIRING_PROOF_ACCOUNT, proof)

    override fun clearPairingDeliveryProof() = clear(PAIRING_PROOF_ACCOUNT)

    override fun loadPairedServer(): String? = load(PAIRED_SERVER_ACCOUNT)

    override fun savePairedServer(server: String) = save(PAIRED_SERVER_ACCOUNT, server)

    override fun clearPairedServer() = clear(PAIRED_SERVER_ACCOUNT)

    private fun load(accountName: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(accountName,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        ) { SecItemCopyMatching(it, result.ptr) }
        if (status != errSecSuccess) {
            return@memScoped null
        }
        (CFBridgingRelease(result.value) as? NSData)?.let {
            NSString.create(it, NSUTF8StringEncoding)?.asKotlinString()
        }
    }

    private fun save(accountName: String, credential: String) {
        clear(accountName)
        val data = CFBridgingRetain(credential.encodeToByteArray().asNSData())
        try {
            withQuery(accountName, kSecValueData to data) { SecItemAdd(it, null) }
        } finally {
            CFBridgingRelease(data)
        }
    }

    private fun clear(accountName: String) {
        withQuery(accountName) { SecItemDelete(it) }
    }

    private inline fun <T> withQuery(
        accountName: String = ACCOUNT_NAME,
        vararg attributes: Pair<CFStringRef?, CFTypeRef?>,
        operation: (CFDictionaryRef?) -> T
    ): T = memScoped {
        val service = CFBridgingRetain(SERVICE_NAME)
        val account = CFBridgingRetain(accountName)
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to account,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            *attributes
        )
        try {
            operation(query)
        } finally {
            CFBridgingRelease(query)
            CFBridgingRelease(service)
            CFBridgingRelease(account)
        }
    }

    private fun ByteArray.asNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

    private companion object {
        const val SERVICE_NAME = "org.mass.ujudge.reconnect"
        const val ACCOUNT_NAME = "credential"
        const val PAIRING_PROOF_ACCOUNT = "pairing_delivery_proof"
        const val PAIRED_SERVER_ACCOUNT = "paired_server"
    }
}

private fun kotlinx.cinterop.MemScope.cfDictionaryOf(
    vararg items: Pair<CFStringRef?, CFTypeRef?>
): CFDictionaryRef? {
    val keys = allocArrayOf(*items.map { it.first }.toTypedArray())
    val values = allocArrayOf(*items.map { it.second }.toTypedArray())
    return CFDictionaryCreate(
        kCFAllocatorDefault,
        keys.reinterpret(),
        values.reinterpret(),
        items.size.convert(),
        null,
        null
    )
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun NSString.asKotlinString(): String = this as String
