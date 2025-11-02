package org.u_judge_client.ui.popup

/**
 * Main interface for all popup components
 */
interface Popup {
    /**
     * Available modes of current popup mode
     */
    enum class Modes {
        NONE,
        SETTINGS,
        WARNING,
        INFORMATION
    }
}