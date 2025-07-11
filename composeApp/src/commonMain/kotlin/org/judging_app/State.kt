package org.judging_app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController

object State {
    var density: Density? = null

    var navController: NavHostController? = null
    var currentDiscipline: Disciplines? = null
    var currentCategory: Categories? = null
    var judgeSurname: String = ""

    val currentLocale = mutableStateOf(getLocale())
    val currentPopupMode = mutableStateOf(PopupMode.NONE)
    val isConnectedToServer = mutableStateOf(false)
    val isAnimating = mutableStateOf(false)
    val isOffline = mutableStateOf(true)


    enum class Routes(val path: String) {
        ENTRY("entry"),

        DISCIPLINE_SELECT("discipline_select"),
        CATEGORY_SELECT("category_select"),

        KERUGI_MODE("kerugi_mode"),
        TANBON_MODE("tanbon_mode"),
        HOSINSOOL_MODE("hosinsool_mode"),
        FREESTYLE_MODE("freestyle_mode"),

        BACK("");

        override fun toString(): String = path
    }

    enum class Disciplines(val value: String) {
        KERUGI("discipline_kerugi"),
        HOSINSOOL("discipline_hosinsool"),
        TANBON("discipline_tanbon"),
        FREESTYLE_WEAPON("discipline_freestyle_weapon"),
        FREESTYLE_PAIR("discipline_freestyle_pair"),
        FREESTYLE_GROUP("discipline_freestyle_group"),
    }

    enum class Colors(val color: Color) {
        PRIMARY(Color(0xFF7C45E2)),
        SECONDARY(Color(0xFFEFD4FF)),
        SLIDER_TRACK_ACTIVE(Color(0xFFBC9DF6)),
        BUTTON_BLUE(Color(0xBF5500FF)),
        BUTTON_RED(Color(0xBFBB0042)),
        BUTTON_GRAY(Color(0xBF525151)),
        BUTTON_BROWN(Color(0xFF2C2C2C))
    }

    enum class Categories(val value: String) {
        JUNIORS("category_juniors"),
        ADULTS("category_adults")
    }

    enum class PopupMode() {
        NONE,
        SETTINGS,
        WARNING,
        INFORMATION
    }
}