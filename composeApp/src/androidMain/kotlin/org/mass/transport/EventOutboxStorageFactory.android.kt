package org.mass.transport

import android.content.Context

actual fun createEventOutboxStorage(context: Any?): EventOutboxStorage {
    val preferences = requireNotNull(context as? Context).getSharedPreferences("event_outbox", Context.MODE_PRIVATE)
    return object : EventOutboxStorage {
        override fun load(): String? = preferences.getString("journal", null)

        override fun save(value: String) {
            preferences.edit().putString("journal", value).apply()
        }
    }
}
