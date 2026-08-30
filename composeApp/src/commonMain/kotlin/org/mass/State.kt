package org.mass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import com.appstractive.dnssd.DiscoveredService
import com.appstractive.dnssd.key
import org.mass.discovery.ServerDiscoveryStore
import org.mass.entities.Rating
import org.mass.enums.Categories
import org.mass.enums.Disciplines
import org.mass.ui.popup.Popup
import kotlin.collections.getValue
import kotlin.collections.setValue

/**
 * Singleton that controls current state of application
 * @property density device's density of the screen
 * @property navController class, responsible for switching between screens
 * @property judgeSurname surname of user (judge)
 * @property serverAddress Server IPv4 address
 * @property currentDiscipline selected discipline
 * @property currentCategory selected age category
 * @property currentRating rating of current bout/performance
 * @property currentLocale selected interface language
 * @property currentPopupMode active [Popup.Modes] mode
 * @property isConnectedToServer is application connected to server or not
 * @property isAnimating flag for ui animations. True if some animation/transition is active, false otherwise
 * @property isOffline flag of application Mode. If true application's functions, that require server connection,  are locked
 */
object State {
    var density: Density? = null
    var navController: NavHostController? = null
    var judgeSurname by mutableStateOf("")
    var serverAddress by mutableStateOf("")
    var currentError by mutableStateOf("")
    var currentDiscipline: Disciplines? = null
    var currentCategory: Categories? = null
    var currentRating: Rating? by mutableStateOf(null)
    var currentLocale by mutableStateOf(getLocale())

    var currentPopupMode by mutableStateOf(Popup.Modes.NONE)
    val availableServers = ServerDiscoveryStore<DiscoveredService> { it.key }
    var selectedServer: DiscoveredService? by mutableStateOf(null)
    var isConnectedToServer by mutableStateOf(false)
    var isAnimating by mutableStateOf(false)
    var isOffline by mutableStateOf(true)
}
