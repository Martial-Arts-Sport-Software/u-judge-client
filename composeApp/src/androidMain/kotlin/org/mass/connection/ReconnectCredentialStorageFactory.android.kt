package org.mass.connection

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual fun createReconnectCredentialStorage(context: Any?): ReconnectCredentialStorage {
    val preferences = requireNotNull(context as? Context)
        .getSharedPreferences("reconnect_credential", Context.MODE_PRIVATE)
    return AndroidReconnectCredentialStorage(preferences)
}

private class AndroidReconnectCredentialStorage(
    private val preferences: android.content.SharedPreferences
) : ReconnectCredentialStorage {
    override fun load(): String? = preferences.getString(CREDENTIAL_KEY, null)?.let { encrypted ->
        runCatching {
            val (encodedIv, encodedCredential) = encrypted.split(SEPARATOR, limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, decode(encodedIv)))
            }
            cipher.doFinal(decode(encodedCredential)).decodeToString()
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(credential: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val encrypted = "${encode(cipher.iv)}$SEPARATOR${encode(cipher.doFinal(credential.encodeToByteArray()))}"
        preferences.edit().putString(CREDENTIAL_KEY, encrypted).apply()
    }

    override fun clear() {
        preferences.edit().remove(CREDENTIAL_KEY).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE).apply { load(null) }
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }
            .generateKey()
    }

    private fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val CREDENTIAL_KEY = "credential"
        const val KEY_ALIAS = "org.mass.reconnect_credential"
        const val KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val SEPARATOR = ":"
    }
}
