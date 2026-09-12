package org.mass.rating

import platform.Foundation.NSUserDefaults

actual fun createRatingDraftStorage(context: Any?): RatingDraftStorage {
    val defaults = NSUserDefaults.standardUserDefaults
    return object : RatingDraftStorage {
        override fun load(): String? = defaults.stringForKey("rating_drafts")

        override fun save(value: String) {
            defaults.setObject(value, "rating_drafts")
        }
    }
}
