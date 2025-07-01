package org.judging_app.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import org.judging_app.State

interface Screen {
    /**
     * Function for drawing screen's UI
     * @param controller - app's [NavController] for navigation between screens
     */
    @Composable
    fun load(controller: NavController)

    @Composable
    fun pxToDp(px: Int): Dp {
        val density = LocalDensity.current
        return with(density) { px.toDp() }
    }
}