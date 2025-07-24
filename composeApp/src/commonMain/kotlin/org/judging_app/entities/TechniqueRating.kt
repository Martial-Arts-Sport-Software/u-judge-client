package org.judging_app.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

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