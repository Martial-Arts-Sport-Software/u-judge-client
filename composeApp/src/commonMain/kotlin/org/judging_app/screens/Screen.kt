package org.judging_app.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface Screen {
    /**
     * Function for drawing screen's UI
     * @param controller - app's [NavController] for navigation between screens
     */
    @Composable
    fun load(controller: NavController)
}