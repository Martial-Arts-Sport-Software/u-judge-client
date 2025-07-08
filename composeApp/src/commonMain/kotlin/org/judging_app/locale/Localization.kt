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
            "entry_judge_surname" to "Judge surname",

            "discipline_title" to "Choose discipline",
            "discipline_kerugi" to "Kerugi",
            "discipline_hosinsool" to "Hosinsool",
            "discipline_tanbon" to "Tanbon",
            "discipline_freestyle_weapon" to "Freestyle with weapon",
            "discipline_freestyle_pair" to "Pair freestyle",
            "discipline_freestyle_group" to "Group freestyle",

            "category_title" to "Choose category",
            "category_juniors" to "Younger juniors / cadets",
            "category_adults" to "Juniors / Adults",

            "kerugi_bout" to "Bout",
            "kerugi_judge" to "Judge",
            "kerugi_judge_empty" to "Not specified",

            "settings_title" to "Settings",
            "settings_start_fight" to "Start a new fight",
            "settings_choose_category" to "Choose category",
            "settings_choose_discipline" to "Choose discipline",

            "warning_title" to "Arbitrator's attention",
            "warning_continue" to "Continue",
        ),
        "ru" to mapOf(
            "entry_club_name" to "СПОРТИВНЫЙ\nКЛУБ Ю'ПИТЕР",
            "entry_club_description" to "Олимпийское Тхэквондо\n" +
                    "Спортивное и боевое Хапкидо",
            "entry_login" to "Онлайн",
            "entry_offline" to "Офлайн",
            "entry_server_address" to "Адрес сервера",
            "entry_judge_surname" to "Фамилия судьи",

            "discipline_title" to "Выберите дисциплину",
            "discipline_kerugi" to "Весовые категории",
            "discipline_hosinsool" to "Приёмы самообороны",
            "discipline_tanbon" to "Танбон",
            "discipline_freestyle_weapon" to "Комплекс свободный",
            "discipline_freestyle_pair" to "Поединок постановочный - пара",
            "discipline_freestyle_group" to "Поединок постановочный - группа",

            "category_title" to "Выберите категорию",
            "category_juniors" to "Младшие юноши / Кадеты",
            "category_adults" to "Юниоры / Взрослые",

            "kerugi_bout" to "Поединок",
            "kerugi_judge" to "Судья",
            "kerugi_judge_empty" to "Не указан",

            "settings_title" to "Настройки",
            "settings_start_fight" to "Начать новый поединок",
            "settings_choose_category" to "Выбрать категорию",
            "settings_choose_discipline" to "Выбрать дисциплину",

            "warning_title" to "Поднятая рука",
            "warning_continue" to "Продолжить",
        )
    )
    fun getString(key: String): String {
        return strings[State.currentLocale.value]
            ?.get(key) ?: strings["ru"]?.get(key) ?: key
    }
}