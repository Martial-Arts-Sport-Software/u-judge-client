package org.judging_app.screens

import androidx.compose.runtime.Composable

/**
 * Main screen interface
 */
interface Screen {
    /**
     * Function for drawing screen's UI
     */
    @Composable
    fun load()
}