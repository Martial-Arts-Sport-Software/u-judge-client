package org.mass.rating

import org.mass.entities.PresentationCriteria
import org.mass.entities.TechniqueCriteria
import org.mass.entities.TechniqueRating
import kotlin.math.roundToInt

/**
 * One judge's vector in integer tenths (server ADR-011 §7-§8, `CLI-050`): criteria `1..10` (`0.1..1.0`) and the judge's
 * own deduction, a multiple of `3` (`0.3`). The total uses the server's arithmetic, `J = max(0, ΣT + ΣP − D)`, so the
 * same payload gives the same total on both sides (`TEC-007`).
 */
data class TechnicalVector(val techniqueTenths: List<Int>, val presentationTenths: List<Int>, val deductionTenths: Int) {
    val techniqueTotalTenths: Int
        get() = techniqueTenths.sum()

    val presentationTotalTenths: Int
        get() = presentationTenths.sum()

    val totalTenths: Int
        get() = maxOf(0, techniqueTotalTenths + presentationTotalTenths - deductionTenths)

    /** True when the vector has the shape the server snapshot announced. */
    fun fits(techniqueCriteria: Int, presentationCriteria: Int): Boolean {
        val maximum = (techniqueCriteria + presentationCriteria) * CRITERION_MAX
        return techniqueTenths.size == techniqueCriteria && presentationTenths.size == presentationCriteria &&
            (techniqueTenths + presentationTenths).all { it in CRITERION_MIN..CRITERION_MAX } &&
            deductionTenths in 0..maximum && deductionTenths % DEDUCTION_STEP == 0
    }

    companion object {
        const val CRITERION_MIN = 1
        const val CRITERION_MAX = 10
        const val DEDUCTION_STEP = 3

        /** A Float score of the screens as tenths; the screens only hold multiples of `0.1`. */
        fun tenths(value: Float): Int = (value * 10).roundToInt()

        /** The vector of a rating in the server's criterion order (ADR-011 §4); `extraPoints` is the negative deduction. */
        fun of(rating: TechniqueRating): TechnicalVector = TechnicalVector(
            techniqueTenths = techniqueScores(rating.techniqueCriteria).map(::tenths),
            presentationTenths = presentationScores(rating.presentationCriteria).map(::tenths),
            deductionTenths = maxOf(0, -tenths(rating.extraPoints))
        )

        fun techniqueScores(criteria: TechniqueCriteria): List<Float> = when (criteria) {
            is TechniqueCriteria.Adult -> listOf(
                criteria.wristHold, criteria.clothesHold, criteria.fistPunch, criteria.legKick,
                criteria.knifeLock, criteria.weaponLock
            )
            is TechniqueCriteria.Junior -> listOf(criteria.wristHold, criteria.clothesHold, criteria.fistPunch, criteria.legKick)
            is TechniqueCriteria.Group -> listOf(
                criteria.offenseDefense, criteria.itemsBreaking, criteria.legKicks, criteria.weaponSkills,
                criteria.dynamicMovement, criteria.acrobatics
            )
            is TechniqueCriteria.Weapon -> listOf(
                criteria.weaponTechniques, criteria.jumpKicks, criteria.rotateKicks, criteria.weaponManipulation,
                criteria.movement, criteria.acrobatics
            )
        }

        fun presentationScores(criteria: PresentationCriteria): List<Float> = when (criteria) {
            is PresentationCriteria.Hosinsool -> listOf(criteria.realism, criteria.power, criteria.balance, criteria.harmony)
            is PresentationCriteria.FreestylePair -> listOf(criteria.creativity, criteria.power, criteria.balance, criteria.choreography)
        }

        /** Tenths as the screens print them: `6.4`. */
        fun format(tenths: Int): String = "${tenths / 10}.${tenths % 10}"
    }
}
