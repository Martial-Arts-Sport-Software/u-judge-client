package org.judging_app

import androidx.compose.runtime.mutableStateOf

object State {
    var currentScreen: Routes = Routes.ENTRY
    var currentLocale = mutableStateOf(getLocale())

    enum class Routes(val path: String) {
        ENTRY("entry"),
        DISCIPLINE_MODE("discipline_mode");

        override fun toString(): String = path
    }
}