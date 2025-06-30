package org.judging_app.locale

import org.judging_app.State

object Localization {
    private val strings = mapOf(
        "en" to mapOf(
            "entry_club_name" to "U'PITER\nSPORT CLUB",
            "entry_club_description" to "Olympic Taekwondo\n" +
                    "Sport and combat Hapkido"
        ),
        "ru" to mapOf(
            "entry_club_name" to "СПОРТИВНЫЙ\nКЛУБ Ю'ПИТЕР",
            "entry_club_description" to "Олимпийское Тхэквондо\n" +
                    "Спортивное и боевое Хапкидо"
        )
    )
    fun getString(key: String): String {
        return strings[State.currentLocale.value]?.get(key) ?: strings["ru"]?.get(key) ?: key
    }
}