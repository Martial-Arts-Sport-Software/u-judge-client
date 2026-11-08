package org.mass.screens

import org.mass.State
import org.mass.enums.Categories
import org.mass.enums.Disciplines
import org.mass.session.TechnicalSnapshot

/**
 * Binds a technical screen to the server's current performance of its discipline (server ADR-011 §6): the snapshot fixes
 * the number of technique criteria (`CLI-021`, `CLI-042`), and a new performance starts a fresh rating. Without a server
 * performance the screen keeps the offline Juniors/Adults choice.
 */
object TechnicalPerformanceBinding {
    private var boundSessionId: String? = null

    /** The server performance this screen scores, or null offline or while another discipline is current. */
    fun performance(discipline: Disciplines?): TechnicalSnapshot? =
        State.session.technical?.takeIf { discipline != null && it.discipline?.discipline == discipline }

    /** Call before the screen builds its rating. */
    fun bind(discipline: Disciplines?) {
        val performance = performance(discipline) ?: return
        State.currentCategory = categoryFor(discipline, performance.techniqueCriteria, State.currentCategory)
        if (boundSessionId != performance.sessionId) {
            boundSessionId = performance.sessionId
            State.currentRating = null
        }
    }

    /** Hosinsool and Pair: four criteria are the Juniors form, six the Adults one; the other disciplines always have six. */
    fun categoryFor(discipline: Disciplines?, techniqueCriteria: Int, current: Categories?): Categories = when (discipline) {
        Disciplines.HOSINSOOL, Disciplines.FREESTYLE_PAIR -> if (techniqueCriteria == 4) Categories.JUNIORS else Categories.ADULTS
        else -> current ?: Categories.ADULTS
    }
}
