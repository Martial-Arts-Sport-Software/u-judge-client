package org.mass.rating

import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mass.entities.PresentationCriteria
import org.mass.entities.TechniqueCriteria
import org.mass.entities.TechniqueRating
import org.mass.enums.Categories
import org.mass.enums.Disciplines

interface RatingDraftStorage {
    fun load(): String?

    fun save(value: String)
}

class RatingDraftRepository(private val storage: RatingDraftStorage) {
    private val drafts = decode(storage.load()).toMutableMap()

    fun save(discipline: Disciplines, category: Categories, rating: TechniqueRating): Boolean {
        val draft = RatingDraft.from(discipline, category, rating) ?: return false
        drafts[DraftKey(discipline, category)] = draft
        persist()
        return true
    }

    fun load(discipline: Disciplines, category: Categories): TechniqueRating? =
        drafts[DraftKey(discipline, category)]?.toRating()

    private fun persist() {
        storage.save(buildJsonArray {
            drafts.forEach { (key, draft) ->
                add(buildJsonObject {
                    put("discipline", key.discipline.name)
                    put("category", key.category.name)
                    put("technique", buildJsonArray { draft.technique.forEach { add(JsonPrimitive(it)) } })
                    put("presentation", buildJsonArray { draft.presentation.forEach { add(JsonPrimitive(it)) } })
                    put("extraPoints", draft.extraPoints)
                    put("totalScore", draft.totalScore)
                })
            }
        }.toString())
    }

    private fun decode(serialized: String?): Map<DraftKey, RatingDraft> = try {
        Json.parseToJsonElement(serialized.orEmpty()).jsonArray.mapNotNull { element ->
            val value = element.jsonObject
            val discipline = value["discipline"]?.jsonPrimitive?.contentOrNull
                ?.let { runCatching { Disciplines.valueOf(it) }.getOrNull() }
                ?: return@mapNotNull null
            val category = value["category"]?.jsonPrimitive?.contentOrNull
                ?.let { runCatching { Categories.valueOf(it) }.getOrNull() }
                ?: return@mapNotNull null
            val technique = value["technique"]?.jsonArray?.map { it.jsonPrimitive.content.toFloatOrNull() }
                ?.takeIf { it.none { score -> score == null } }?.filterNotNull()
                ?: return@mapNotNull null
            val presentation = value["presentation"]?.jsonArray?.map { it.jsonPrimitive.content.toFloatOrNull() }
                ?.takeIf { it.none { score -> score == null } }?.filterNotNull()
                ?: return@mapNotNull null
            val extraPoints = value["extraPoints"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return@mapNotNull null
            val totalScore = value["totalScore"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return@mapNotNull null
            RatingDraft(discipline, category, technique, presentation, extraPoints, totalScore)
                .takeIf { it.toRating() != null }
                ?.let { DraftKey(discipline, category) to it }
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }

    private data class DraftKey(val discipline: Disciplines, val category: Categories)

    private data class RatingDraft(
        val discipline: Disciplines,
        val category: Categories,
        val technique: List<Float>,
        val presentation: List<Float>,
        val extraPoints: Float,
        val totalScore: Float
    ) {
        fun toRating(): TechniqueRating? {
            if (!hasValidShape()) return null
            val rating = runCatching {
                when (discipline) {
                Disciplines.HOSINSOOL -> TechniqueRating(
                    discipline.name,
                    juniorOrAdultTechnique(),
                    PresentationCriteria.Hosinsool(presentation[0], presentation[1], presentation[2], presentation[3]),
                    extraPoints
                )
                Disciplines.FREESTYLE_PAIR -> TechniqueRating(
                    discipline.name,
                    juniorOrAdultTechnique(),
                    PresentationCriteria.FreestylePair(presentation[0], presentation[1], presentation[2], presentation[3]),
                    extraPoints
                )
                Disciplines.FREESTYLE_GROUP -> TechniqueRating(
                    discipline.name,
                    TechniqueCriteria.Group(technique[0], technique[1], technique[2], technique[3], technique[4], technique[5]),
                    PresentationCriteria.FreestyleGroup(presentation[0], presentation[1], presentation[2], presentation[3]),
                    extraPoints
                )
                Disciplines.FREESTYLE_WEAPON -> TechniqueRating(
                    discipline.name,
                    TechniqueCriteria.Weapon(technique[0], technique[1], technique[2], technique[3], technique[4], technique[5]),
                    PresentationCriteria.FreestyleWeapon(presentation[0], presentation[1], presentation[2], presentation[3]),
                    extraPoints
                )
                Disciplines.KERUGI, Disciplines.TANBON -> return null
                }
            }.getOrNull() ?: return null
            return rating.takeIf { abs(it.totalScore - totalScore) < 0.001f }
        }

        private fun juniorOrAdultTechnique(): TechniqueCriteria = when (category) {
            Categories.JUNIORS -> TechniqueCriteria.Junior(technique[0], technique[1], technique[2], technique[3])
            Categories.ADULTS -> TechniqueCriteria.Adult(
                technique[0], technique[1], technique[2], technique[3], technique[4], technique[5]
            )
        }

        private fun hasValidShape(): Boolean {
            val expectedTechniqueCount = when (discipline) {
                Disciplines.HOSINSOOL, Disciplines.FREESTYLE_PAIR -> if (category == Categories.JUNIORS) 4 else 6
                Disciplines.FREESTYLE_GROUP, Disciplines.FREESTYLE_WEAPON -> 6
                Disciplines.KERUGI, Disciplines.TANBON -> return false
            }
            return technique.size == expectedTechniqueCount && presentation.size == 4 &&
                technique.all(::isCriterionScore) && presentation.all(::isCriterionScore) &&
                extraPoints.isFinite() && totalScore.isFinite() && totalScore >= 0f
        }

        private fun isCriterionScore(value: Float): Boolean =
            value.isFinite() && value in 0.1f..1f && abs(value * 10 - value.times(10).toInt()) < 0.001f

        companion object {
            fun from(
                discipline: Disciplines,
                category: Categories,
                rating: TechniqueRating
            ): RatingDraft? {
                val technique = when (val criteria = rating.techniqueCriteria) {
                    is TechniqueCriteria.Adult -> listOf(
                        criteria.wristHold, criteria.clothesHold, criteria.fistPunch, criteria.legKick,
                        criteria.knifeLock, criteria.weaponLock
                    )
                    is TechniqueCriteria.Junior -> listOf(
                        criteria.wristHold, criteria.clothesHold, criteria.fistPunch, criteria.legKick
                    )
                    is TechniqueCriteria.Group -> listOf(
                        criteria.offenseDefense, criteria.itemsBreaking, criteria.legKicks, criteria.weaponSkills,
                        criteria.dynamicMovement, criteria.acrobatics
                    )
                    is TechniqueCriteria.Weapon -> listOf(
                        criteria.weaponTechniques, criteria.jumpKicks, criteria.rotateKicks,
                        criteria.weaponManipulation, criteria.movement, criteria.acrobatics
                    )
                }
                val presentation = when (val criteria = rating.presentationCriteria) {
                    is PresentationCriteria.Hosinsool -> listOf(
                        criteria.realism, criteria.power, criteria.balance, criteria.harmony
                    )
                    is PresentationCriteria.FreestylePair -> listOf(
                        criteria.creativity, criteria.power, criteria.balance, criteria.choreography
                    )
                }
                return RatingDraft(
                    discipline,
                    category,
                    technique,
                    presentation,
                    rating.extraPoints,
                    rating.totalScore
                ).takeIf { it.toRating() != null }
            }
        }
    }
}
