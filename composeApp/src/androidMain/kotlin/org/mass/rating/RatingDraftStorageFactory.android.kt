package org.mass.rating

import android.content.Context

actual fun createRatingDraftStorage(context: Any?): RatingDraftStorage {
    val preferences = requireNotNull(context as? Context).getSharedPreferences("rating_drafts", Context.MODE_PRIVATE)
    return object : RatingDraftStorage {
        override fun load(): String? = preferences.getString("drafts", null)

        override fun save(value: String) {
            preferences.edit().putString("drafts", value).apply()
        }
    }
}
