package org.mass.connection

import platform.Foundation.NSUserDefaults

actual fun createPairingIdentityStorage(context: Any?): PairingIdentityStorage {
    val defaults = NSUserDefaults.standardUserDefaults
    return object : PairingIdentityStorage {
        override fun get(key: String): String? = defaults.stringForKey(key)

        override fun put(key: String, value: String) {
            defaults.setObject(value, key)
        }
    }
}
