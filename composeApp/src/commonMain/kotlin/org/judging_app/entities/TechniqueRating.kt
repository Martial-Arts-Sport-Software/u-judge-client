package org.judging_app.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

/**
 * Parent for technique ratings - hosinsool, freestyle (all categories)
 * @property techniqueCriteria - score for technique part, variant of [TechniqueCriteria]
 * @property presentationCriteria - score for presentation part, variant of [PresentationCriteria]
 */
class TechniqueRating(
    id: String,
    techniqueCriteria: TechniqueCriteria,
    presentationCriteria: PresentationCriteria
): Rating(id) {
    val techniqueCriteria by mutableStateOf(techniqueCriteria)
    val presentationCriteria by mutableStateOf(presentationCriteria)
    override fun toString(): String {
        return "id: $id,\n" +
                "techniqueCriteria: $techniqueCriteria,\n" +
                "presentationCriteria: $presentationCriteria"
    }
}