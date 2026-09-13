package org.mass.rating

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mass.entities.PresentationCriteria
import org.mass.entities.TechniqueCriteria
import org.mass.entities.TechniqueRating
import org.mass.enums.Categories
import org.mass.enums.Disciplines

class RatingDraftRepositoryTest {
    @Test
    fun savesAndRestoresAnAdultHosinsoolDraftAfterRecreation() {
        val storage = MemoryStorage()
        val rating = TechniqueRating(
            "hosinsool",
            TechniqueCriteria.Adult(0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f),
            PresentationCriteria.Hosinsool(0.8f, 0.9f, 1f, 0.1f),
            exPoints = -0.3f
        )

        assertTrue(RatingDraftRepository(storage).save(Disciplines.HOSINSOOL, Categories.ADULTS, rating))

        val restored = requireNotNull(
            RatingDraftRepository(storage).load(Disciplines.HOSINSOOL, Categories.ADULTS)
        )
        val technique = restored.techniqueCriteria as TechniqueCriteria.Adult
        val presentation = restored.presentationCriteria as PresentationCriteria.Hosinsool
        assertEquals(0.6f, technique.knifeLock)
        assertEquals(0.7f, technique.weaponLock)
        assertEquals(0.8f, presentation.realism)
        assertEquals(-0.3f, restored.extraPoints)
        assertEquals(rating.totalScore, restored.totalScore)
    }

    @Test
    fun keepsDraftsSeparatedByDisciplineAndCategory() {
        val storage = MemoryStorage()
        val repository = RatingDraftRepository(storage)
        val rating = TechniqueRating(
            "pair",
            TechniqueCriteria.Junior(0.2f, 0.3f, 0.4f, 0.5f),
            PresentationCriteria.FreestylePair(0.6f, 0.7f, 0.8f, 0.9f)
        )

        assertTrue(repository.save(Disciplines.FREESTYLE_PAIR, Categories.JUNIORS, rating))

        assertNull(repository.load(Disciplines.FREESTYLE_PAIR, Categories.ADULTS))
        assertNull(repository.load(Disciplines.HOSINSOOL, Categories.JUNIORS))
    }

    @Test
    fun restoresIndependentDraftsForEveryWeaponDiscipline() {
        val storage = MemoryStorage()
        val repository = RatingDraftRepository(storage)
        val disciplines = listOf(
            Disciplines.FREESTYLE_SWORD,
            Disciplines.FREESTYLE_POLE,
            Disciplines.FREESTYLE_NUNCHAKU,
            Disciplines.FREESTYLE_FANS
        )

        disciplines.forEachIndexed { index, discipline ->
            assertTrue(repository.save(discipline, Categories.ADULTS, weaponRating(0.1f * (index + 1))))
        }

        val restored = RatingDraftRepository(storage)
        disciplines.forEachIndexed { index, discipline ->
            val rating = requireNotNull(restored.load(discipline, Categories.ADULTS))
            val technique = rating.techniqueCriteria as TechniqueCriteria.Weapon
            assertEquals(0.1f * (index + 1), technique.weaponTechniques)
            assertEquals(0.8f, (rating.presentationCriteria as PresentationCriteria.FreestyleWeapon).choreography)
        }
        assertNull(restored.load(Disciplines.FREESTYLE_SWORD, Categories.JUNIORS))
    }

    @Test
    fun ignoresMalformedOrInvalidPersistedDrafts() {
        val storage = MemoryStorage(
            """[{"discipline":"HOSINSOOL","category":"JUNIORS","technique":[0.2,0.3,0.4,0.5],"presentation":[0.6,0.7,0.8,0.9],"extraPoints":0,"totalScore":99}]"""
        )

        assertNull(RatingDraftRepository(storage).load(Disciplines.HOSINSOOL, Categories.JUNIORS))
        assertFalse(storage.value.isNullOrBlank())
    }

    @Test
    fun rejectsWeaponDraftWithTheWrongCriteriaShape() {
        val storage = MemoryStorage(
            """[{"discipline":"FREESTYLE_SWORD","category":"ADULTS","technique":[0.2,0.3,0.4,0.5],"presentation":[0.6,0.7,0.8,0.9],"extraPoints":0,"totalScore":2.4}]"""
        )

        assertNull(RatingDraftRepository(storage).load(Disciplines.FREESTYLE_SWORD, Categories.ADULTS))
    }

    private fun weaponRating(firstCriterion: Float): TechniqueRating = TechniqueRating(
        "weapon",
        TechniqueCriteria.Weapon(firstCriterion, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f),
        PresentationCriteria.FreestyleWeapon(0.5f, 0.6f, 0.7f, 0.8f)
    )

    private class MemoryStorage(var value: String? = null) : RatingDraftStorage {
        override fun load(): String? = value

        override fun save(value: String) {
            this.value = value
        }
    }
}
