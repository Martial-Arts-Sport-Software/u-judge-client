package org.mass.connection

import android.content.Context

actual fun createPairingIdentityStorage(context: Any?): PairingIdentityStorage {
    val preferences = requireNotNull(context as? Context).getSharedPreferences("pairing_identity", Context.MODE_PRIVATE)
    return object : PairingIdentityStorage {
        override fun get(key: String): String? = preferences.getString(key, null)

        override fun put(key: String, value: String) {
            preferences.edit().putString(key, value).apply()
        }
    }
}
