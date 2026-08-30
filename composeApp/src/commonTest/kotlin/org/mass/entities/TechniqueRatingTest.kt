package org.mass.entities

import kotlin.test.Test
import kotlin.test.assertEquals

class TechniqueRatingTest {
    @Test
    fun totalScoreRoundsToOneDecimalPlace() {
        val rating = TechniqueRating(
            id = "rating-1",
            techniqueCriteria = TechniqueCriteria.Junior(),
            presentationCriteria = PresentationCriteria.Hosinsool(),
            exPoints = 0.25f
        )

        assertEquals(1.1f, rating.totalScore)
    }

    @Test
    fun totalScoreUpdatesWhenCriteriaOrExtraPointsChange() {
        val technique = TechniqueCriteria.Junior()
        val presentation = PresentationCriteria.Hosinsool()
        val rating = TechniqueRating("rating-1", technique, presentation)

        assertEquals(0.8f, rating.totalScore)

        technique.wristHold = 0.5f
        presentation.harmony = 0.7f
        rating.extraPoints = 0.25f

        assertEquals(2.1f, rating.totalScore)
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
