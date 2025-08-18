package org.judging_app.entities

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.round

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
    val totalScore by derivedStateOf {
        val sum = round((extraPoints +
                techniqueCriteria.getTotalScore() +
                presentationCriteria.getTotalScore()) * 10) / 10f
        if (sum < 0f) return@derivedStateOf 0f
        return@derivedStateOf sum
    }
    override fun toString(): String {
        return "id: $id,\n" +
                "techniqueCriteria: $techniqueCriteria,\n" +
                "presentationCriteria: $presentationCriteria,\n" +
                "extraPoints: $extraPoints,\n" +
                "totalScore: $totalScore"
    }
}