package org.judging_app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import org.judging_app.locale.Localization

object State {
    var navController: NavHostController? = null
    var currentLocale = mutableStateOf(getLocale())
    var currentDiscipline: Disciplines? = null
    var density: Density? = null
    var screenWidthPx: Float? = null
    var screenHeightPx: Float? = null
    var isAnimating = mutableStateOf(false)
    var judgeSurname: String = ""


    enum class Routes(val path: String) {
        ENTRY("entry"),
        DISCIPLINE_MODE("discipline_mode"),
        CATEGORY_MODE("category_mode"),
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
        SECONDARY(Color(0xFFEFD4FF))
    }
}