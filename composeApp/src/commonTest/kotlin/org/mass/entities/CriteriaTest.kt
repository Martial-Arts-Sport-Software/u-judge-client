package org.mass.entities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CriteriaTest {
    @Test
    fun techniqueCriteriaTotalsIncludeEveryCriterion() {
        assertEquals(1.0f, TechniqueCriteria.Junior(0.1f, 0.2f, 0.3f, 0.4f).getTotalScore())
        assertEquals(
            2.1f,
            TechniqueCriteria.Adult(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f).getTotalScore()
        )
        assertEquals(
            2.1f,
            TechniqueCriteria.Group(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f).getTotalScore()
        )
        assertEquals(
            2.1f,
            TechniqueCriteria.Weapon(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f).getTotalScore()
        )
    }

    @Test
    fun techniqueCriteriaRejectScoresOutsideAllowedRange() {
        assertEquals(0.4f, TechniqueCriteria.Junior().getTotalScore())
        assertEquals(6.0f, TechniqueCriteria.Adult(1f, 1f, 1f, 1f, 1f, 1f).getTotalScore())
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Junior(wristHold = 0f) }
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Adult(knifeLock = 1.1f) }
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Group(acrobatics = 0f) }
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Weapon(weaponTechniques = 1.1f) }
    }

    @Test
    fun techniqueCriteriaEnforceTenthsAfterConstruction() {
        val junior = TechniqueCriteria.Junior()
        junior.wristHold = 0.7f
        assertEquals(0.7f, junior.wristHold)
        listOf(0f, 0.15f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { score ->
            assertFailsWith<IllegalArgumentException> { junior.wristHold = score }
        }
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Adult().knifeLock = 0f }
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Group().acrobatics = 0.15f }
        assertFailsWith<IllegalArgumentException> { TechniqueCriteria.Weapon().movement = Float.NEGATIVE_INFINITY }
    }

    @Test
    fun validCriterionMutationRecalculatesRatingTotal() {
        val technique = TechniqueCriteria.Junior()
        val rating = TechniqueRating(
            "rating",
            technique,
            PresentationCriteria.Hosinsool()
        )

        technique.wristHold = 1f

        assertEquals(1.7f, rating.totalScore)
    }

    @Test
    fun presentationCriteriaTotalsIncludeEveryCriterion() {
        assertEquals(1.0f, PresentationCriteria.Hosinsool(0.1f, 0.2f, 0.3f, 0.4f).getTotalScore())
        assertEquals(1.0f, PresentationCriteria.FreestylePair(0.1f, 0.2f, 0.3f, 0.4f).getTotalScore())
        assertEquals(1.0f, PresentationCriteria.FreestyleGroup(0.1f, 0.2f, 0.3f, 0.4f).getTotalScore())
        assertEquals(1.0f, PresentationCriteria.FreestyleWeapon(0.1f, 0.2f, 0.3f, 0.4f).getTotalScore())
    }

    @Test
    fun presentationCriteriaRejectScoresOutsideAllowedRange() {
        assertEquals(0.4f, PresentationCriteria.Hosinsool().getTotalScore())
        assertEquals(4.0f, PresentationCriteria.FreestylePair(1f, 1f, 1f, 1f).getTotalScore())
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.Hosinsool(realism = 0f) }
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.FreestylePair(creativity = 1.1f) }
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.FreestyleGroup(balance = 0f) }
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.FreestyleWeapon(choreography = 1.1f) }
    }

    @Test
    fun presentationCriteriaEnforceTenthsAfterConstruction() {
        val hosinsool = PresentationCriteria.Hosinsool()
        hosinsool.realism = 1f
        assertEquals(1f, hosinsool.realism)
        listOf(0f, 0.15f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { score ->
            assertFailsWith<IllegalArgumentException> { hosinsool.realism = score }
        }
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.FreestylePair().power = 0f }
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.FreestyleGroup().balance = 0.15f }
        assertFailsWith<IllegalArgumentException> { PresentationCriteria.FreestyleWeapon().choreography = Float.NEGATIVE_INFINITY }
    }
}
