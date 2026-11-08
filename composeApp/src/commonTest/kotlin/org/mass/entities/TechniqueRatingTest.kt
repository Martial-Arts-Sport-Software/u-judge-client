package org.mass.entities

import kotlin.test.Test
import kotlin.test.assertEquals

class TechniqueRatingTest {
    @Test
    fun totalScoreIsTheSumOfTenthsMinusTheJudgesDeduction() {
        val rating = TechniqueRating(
            id = "rating-1",
            techniqueCriteria = TechniqueCriteria.Junior(0.7f, 0.8f, 0.9f, 0.6f),
            presentationCriteria = PresentationCriteria.Hosinsool(0.7f, 0.8f, 0.8f, 0.9f),
            exPoints = -0.3f
        )

        assertEquals(59, rating.totalTenths)
        assertEquals(5.9f, rating.totalScore)
    }

    @Test
    fun totalScoreUpdatesWhenCriteriaOrExtraPointsChange() {
        val technique = TechniqueCriteria.Junior()
        val presentation = PresentationCriteria.Hosinsool()
        val rating = TechniqueRating("rating-1", technique, presentation)

        assertEquals(0.8f, rating.totalScore)

        technique.wristHold = 0.5f
        presentation.harmony = 0.7f
        rating.extraPoints = -0.3f

        assertEquals(1.5f, rating.totalScore)
    }

    @Test
    fun positiveExtraPointsAreNotAJudgeBonus() {
        // Bonuses are the arbiter's adjustments on the desktop (server ADR-011 §8), never part of the judge's vector.
        val rating = TechniqueRating("rating-1", TechniqueCriteria.Junior(), PresentationCriteria.Hosinsool(), exPoints = 0.3f)

        assertEquals(0.8f, rating.totalScore)
    }

    @Test
    fun totalScoreCannotBeNegative() {
        val rating = TechniqueRating(
            id = "rating-1",
            techniqueCriteria = TechniqueCriteria.Junior(),
            presentationCriteria = PresentationCriteria.Hosinsool(),
            exPoints = -1f
        )

        assertEquals(0f, rating.totalScore)
    }
}
