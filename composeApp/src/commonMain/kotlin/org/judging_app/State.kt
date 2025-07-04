package org.judging_app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController

object State {
    var navController: NavHostController? = null
    var currentLocale = mutableStateOf(getLocale())
    var density: Density? = null
    var screenWidthPx: Float? = null
    var screenHeightPx: Float? = null
    var isAnimating = mutableStateOf(false)
    var judgeSurname: String = ""
    enum class Routes(val path: String) {
        ENTRY("entry"),
        DISCIPLINE_MODE("discipline_mode");

        override fun toString(): String = path
    }
}