package org.mass.rating

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.contract.ServerContractFixtures
import org.mass.entities.PresentationCriteria
import org.mass.entities.TechniqueCriteria
import org.mass.entities.TechniqueRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `CLI-045`, `CLI-050`, server `TEC-007`: the client's local total of every judge of the server's normative vectors
 * (fixture `technical_test_vectors` at `contract/server-ref`) equals the server's, through the screens' rating model.
 */
class TechnicalVectorTest {
    @Test
    fun everyJudgeOfTheServerVectorsGetsTheServerTotalOnTheClient() {
        val vectors = Json.parseToJsonElement(ServerContractFixtures.message("technical_test_vectors"))
            .jsonObject.getValue("vectors").jsonArray.map { it.jsonObject }
        assertTrue(vectors.size >= 10)
        assertEquals(
            setOf("hosinsool", "pair_freestyle", "group_freestyle", "sword", "pole", "paired_nunchaku", "paired_fans"),
            vectors.map { it.text("discipline") }.toSet()
        )
        vectors.forEach { vector ->
            val discipline = vector.text("discipline")
            val techniqueCount = vector.getValue("techniqueCriteria").jsonPrimitive.int
            vector.getValue("judges").jsonArray.map { it.jsonObject }.forEachIndexed { index, judge ->
                val name = "${vector.text("name")} judge ${index + 1}"
                val expected = TechnicalVector(judge.ints("techniqueTenths"), judge.ints("presentationTenths"), judge.int("deductionTenths"))
                assertTrue(expected.fits(techniqueCount, 4), name)
                assertEquals(judge.int("totalTenths"), expected.totalTenths, name)

                val rating = rating(discipline, expected)
                assertEquals(expected, TechnicalVector.of(rating), "$name vector")
                assertEquals(judge.int("totalTenths"), rating.totalTenths, "$name rating total")
            }
        }
    }

    @Test
    fun theShapeFollowsTheSnapshotCriteria() {
        assertTrue(TechnicalVector(List(4) { 1 }, List(4) { 1 }, 0).fits(4, 4))
        assertFalse(TechnicalVector(List(4) { 1 }, List(4) { 1 }, 0).fits(6, 4))
        assertFalse(TechnicalVector(listOf(0, 1, 1, 1), List(4) { 1 }, 0).fits(4, 4))
        assertFalse(TechnicalVector(List(4) { 5 }, List(4) { 5 }, 2).fits(4, 4))
        assertEquals("6.4", TechnicalVector.format(64))
        assertEquals("0.0", TechnicalVector.format(0))
        assertEquals("10.0", TechnicalVector.format(100))
    }

    /** The rating the judge's screen holds for [vector]: criteria as `Float`, the deduction as negative extra points. */
    private fun rating(discipline: String, vector: TechnicalVector): TechniqueRating {
        val t = vector.techniqueTenths.map { it / 10f }
        val p = vector.presentationTenths.map { it / 10f }
        val technique = when (discipline) {
            "hosinsool", "pair_freestyle" ->
                if (t.size == 4) TechniqueCriteria.Junior(t[0], t[1], t[2], t[3]) else TechniqueCriteria.Adult(t[0], t[1], t[2], t[3], t[4], t[5])
            "group_freestyle" -> TechniqueCriteria.Group(t[0], t[1], t[2], t[3], t[4], t[5])
            else -> TechniqueCriteria.Weapon(t[0], t[1], t[2], t[3], t[4], t[5])
        }
        val presentation = when (discipline) {
            "hosinsool" -> PresentationCriteria.Hosinsool(p[0], p[1], p[2], p[3])
            "pair_freestyle" -> PresentationCriteria.FreestylePair(p[0], p[1], p[2], p[3])
            "group_freestyle" -> PresentationCriteria.FreestyleGroup(p[0], p[1], p[2], p[3])
            else -> PresentationCriteria.FreestyleWeapon(p[0], p[1], p[2], p[3])
        }
        return TechniqueRating(discipline, technique, presentation, exPoints = -vector.deductionTenths / 10f)
    }

    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

    private fun JsonObject.int(key: String) = getValue(key).jsonPrimitive.int

    private fun JsonObject.ints(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.int }
}
