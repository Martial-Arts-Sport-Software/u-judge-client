package org.judging_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import org.judging_app.entities.Rating
import org.judging_app.enums.Categories
import org.judging_app.enums.Disciplines
import org.judging_app.ui.popup.Popup

/**
 * Singleton that controls current state of application
 * @property density - device's density of the screen
 * @property navController - class, responsible for switching between screens
 * @property judgeSurname - surname of user (judge)
 * @property serverAddress - Server IPv4 address
 * @property currentDiscipline - selected discipline
 * @property currentCategory - selected age category
 * @property currentRating - rating of current bout/performance
 * @property currentLocale - selected interface language
 * @property currentPopupMode - active [Popup.Modes] mode
 * @property isConnectedToServer - is application connected to server or not
 * @property isAnimating - flag for ui animations. True if some animation/transition is active, false otherwise
 * @property isOffline - flag of application Mode. If true application's functions, that require server connection,  are locked
 */
object State {
    var density: Density? = null
    var navController: NavHostController? = null
    var judgeSurname by mutableStateOf("")
    var serverAddress by mutableStateOf("")

    var currentDiscipline: Disciplines? = null
    var currentCategory: Categories? = null
    var currentRating: Rating? by mutableStateOf(null)
    var currentLocale by mutableStateOf(getLocale())
    var currentPopupMode by mutableStateOf(Popup.Modes.NONE)

    var isConnectedToServer by mutableStateOf(false)
    var isAnimating by mutableStateOf(false)
    var isOffline by mutableStateOf(true)
}