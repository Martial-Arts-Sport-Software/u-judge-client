package org.judging_app.locale

import org.judging_app.State

object Localization {
    private val strings = mapOf(
        "en" to mapOf(
            "entry_club_name" to "U'PITER\nSPORT CLUB",
            "entry_club_description" to "Olympic Taekwondo\n" +
                    "Sport and combat Hapkido",
            "entry_login" to "Online",
            "entry_offline" to "Offline",
            "entry_server_address" to "Server address",
            "entry_judge_surname" to "Judge surname"
        ),
        "ru" to mapOf(
            "entry_club_name" to "СПОРТИВНЫЙ\nКЛУБ Ю'ПИТЕР",
            "entry_club_description" to "Олимпийское Тхэквондо\n" +
                    "Спортивное и боевое Хапкидо",
            "entry_login" to "Онлайн",
            "entry_offline" to "Офлайн",
            "entry_server_address" to "Адрес сервера",
            "entry_judge_surname" to "Фамилия судьи"
        )
    )
    fun getString(key: String): String {
        return strings[State.currentLocale.value]?.get(key) ?: strings["ru"]?.get(key) ?: key
    }
}