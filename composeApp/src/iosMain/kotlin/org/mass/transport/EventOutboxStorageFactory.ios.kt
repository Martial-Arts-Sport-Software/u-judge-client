package org.mass.transport

import platform.Foundation.NSUserDefaults

actual fun createEventOutboxStorage(context: Any?): EventOutboxStorage {
    val defaults = NSUserDefaults.standardUserDefaults
    return object : EventOutboxStorage {
        override fun load(): String? = defaults.stringForKey("event_outbox_journal")

        override fun save(value: String) {
            defaults.setObject(value, "event_outbox_journal")
        }
    }
}
