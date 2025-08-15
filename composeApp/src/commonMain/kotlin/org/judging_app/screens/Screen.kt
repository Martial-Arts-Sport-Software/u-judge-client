package org.judging_app.screens

import androidx.compose.runtime.Composable

/**
 * Main screen interface
 * @property load function with [Composable] annotation, that loads the screen
 */
interface Screen {
    /**
     * Function for drawing screen's UI
     */
    @Composable
    fun load()
}