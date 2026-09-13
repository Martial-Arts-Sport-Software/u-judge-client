package org.mass.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mass.enums.Disciplines
import org.mass.enums.Routes

class DisciplineAvailabilityTest {
    @Test
    fun offlineAllowsEveryTechnicalDisciplineAndBlocksCombatDisciplines() {
        val availableOffline = Disciplines.entries.filter { isDisciplineAvailable(it, isOffline = true) }.toSet()

        assertEquals(
            setOf(
                Disciplines.HOSINSOOL,
                Disciplines.FREESTYLE_PAIR,
                Disciplines.FREESTYLE_GROUP,
                Disciplines.FREESTYLE_SWORD,
                Disciplines.FREESTYLE_POLE,
                Disciplines.FREESTYLE_NUNCHAKU,
                Disciplines.FREESTYLE_FANS
            ),
            availableOffline
        )
    }

    @Test
    fun everyDisciplineUsesAnExplicitRouteInsteadOfParsingItsResourceKey() {
        assertEquals(Routes.FREESTYLE_MODE, Disciplines.FREESTYLE_SWORD.route)
        assertEquals(Routes.FREESTYLE_MODE, Disciplines.FREESTYLE_POLE.route)
        assertEquals(Routes.FREESTYLE_MODE, Disciplines.FREESTYLE_NUNCHAKU.route)
        assertEquals(Routes.FREESTYLE_MODE, Disciplines.FREESTYLE_FANS.route)
        assertTrue(Disciplines.entries.all { it.route.path.isNotBlank() })
    }

    @Test
    fun onlineAllowsEverySelectableDiscipline() {
        assertEquals(
            Disciplines.entries.filter(Disciplines::isSelectable).toSet(),
            Disciplines.entries.filter { isDisciplineAvailable(it, isOffline = false) }.toSet()
        )
    }
}
