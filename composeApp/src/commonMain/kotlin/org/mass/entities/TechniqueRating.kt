package org.mass.entities

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mass.rating.TechnicalVector

/**
 * Parent for technique ratings - hosinsool, freestyle (all categories)
 * @property techniqueCriteria score for technique part, variant of [TechniqueCriteria]
 * @property presentationCriteria score for presentation part, variant of [PresentationCriteria]
 */
class TechniqueRating(
    id: String,
    techniqueCriteria: TechniqueCriteria,
    presentationCriteria: PresentationCriteria,
    exPoints: Float = 0f
): Rating(id) {
    val techniqueCriteria by mutableStateOf(techniqueCriteria)
    val presentationCriteria by mutableStateOf(presentationCriteria)
    var extraPoints by mutableStateOf(exPoints)
    /** Locked once the final Send is pending or acknowledged: the judge can no longer change it (`CLI-051`). */
    var locked by mutableStateOf(false)

    /** The judge's total in integer tenths, the server's arithmetic (server ADR-011 §8); never below zero. */
    val totalTenths by derivedStateOf { TechnicalVector.of(this).totalTenths }

    val totalScore by derivedStateOf { totalTenths / 10f }
    override fun toString(): String {
        return "id: $id,\n" +
                "techniqueCriteria: $techniqueCriteria,\n" +
                "presentationCriteria: $presentationCriteria,\n" +
                "extraPoints: $extraPoints,\n" +
                "totalScore: $totalScore"
    }
}
