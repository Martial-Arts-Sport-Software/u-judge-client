package org.mass.screens

import org.mass.enums.Categories
import org.mass.enums.Disciplines
import kotlin.test.Test
import kotlin.test.assertEquals

/** `CLI-021`, `CLI-042`: the server snapshot's criteria count picks the Juniors (4) or Adults (6) technique form. */
class TechnicalPerformanceBindingTest {
    @Test
    fun hosinsoolAndPairFollowTheServerCriteriaCount() {
        listOf(Disciplines.HOSINSOOL, Disciplines.FREESTYLE_PAIR).forEach { discipline ->
            assertEquals(Categories.JUNIORS, TechnicalPerformanceBinding.categoryFor(discipline, 4, Categories.ADULTS))
            assertEquals(Categories.ADULTS, TechnicalPerformanceBinding.categoryFor(discipline, 6, Categories.JUNIORS))
        }
    }

    @Test
    fun theOtherDisciplinesKeepTheirSixCriteriaForm() {
        assertEquals(Categories.JUNIORS, TechnicalPerformanceBinding.categoryFor(Disciplines.FREESTYLE_GROUP, 6, Categories.JUNIORS))
        assertEquals(Categories.ADULTS, TechnicalPerformanceBinding.categoryFor(Disciplines.FREESTYLE_FANS, 6, null))
    }
}
