package org.judging_app.screens

/**
 * Interface of all technique screens - hosinsool, freestyle
 */
interface TechniqueScreen : Screen {
    var currentDisplay: DISPLAY
    var nextDisplay: DISPLAY
    var showInformation: Boolean
    /**
     * Possible display of all technique discipline
     * @property TECHNIQUE - display of technique criteria
     * @property PRESENTATION - display of presentation criteria
     * @property RESULT - display with score sum for technique and presentation separately and total score sum
     */
    enum class DISPLAY {
        TECHNIQUE,
        PRESENTATION,
        RESULT
    }
}