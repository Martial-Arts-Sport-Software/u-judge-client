package org.mass.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.round
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal fun requireCriterionScore(value: Float) {
    require(
        value.isFinite() && value in 0.1f..1f &&
            abs(value * 10 - round(value * 10)) < 0.001f
    )
}

internal fun criterionScoreState(initialValue: Float): ReadWriteProperty<Any?, Float> {
    requireCriterionScore(initialValue)
    var score by mutableStateOf(initialValue)
    return object : ReadWriteProperty<Any?, Float> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float = score

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            requireCriterionScore(value)
            score = value
        }
    }
}

/**
 * rating's score for presentation, has:
 * - [TechniqueCriteria.Junior]
 * - [TechniqueCriteria.Adult]
 */
sealed class TechniqueCriteria {
    /**
     * fun to get total score for all technique criteria
     * @return total sum
     */
    abstract fun getTotalScore(): Float

    /**
     * [TechniqueCriteria] implementation for hosinsool & freestyle pair junior category
     * @property wristHold score of wrist holding defense
     * @property clothesHold score of clothes holding defense
     * @property fistPunch score of fist punch defense
     * @property legKick score of leg kick defense
     */
    open class Junior(
        wristHold: Float = 0.1f,
        clothesHold: Float = 0.1f,
        fistPunch: Float = 0.1f,
        legKick: Float = 0.1f,
    ): TechniqueCriteria() {
        init {
            requireCriterionScore(wristHold)
            requireCriterionScore(clothesHold)
            requireCriterionScore(fistPunch)
            requireCriterionScore(legKick)
        }
        
        var wristHold by criterionScoreState(wristHold)
        var clothesHold by criterionScoreState(clothesHold)
        var fistPunch by criterionScoreState(fistPunch)
        var legKick by criterionScoreState(legKick)

        override fun toString(): String {
            return "wristHold: $wristHold,\n" +
                    "clothesHold: $clothesHold,\n" +
                    "fistPunch: $fistPunch,\n" +
                    "legKick: $legKick"
        }

        override fun getTotalScore(): Float {
            return round((wristHold + clothesHold + fistPunch + legKick) * 10) / 10f
        }
    }

    /**
     * [TechniqueCriteria] implementation for hosinsool & freestyle pair adult category
     * @property wristHold score of wrist holding defense
     * @property clothesHold score of clothes holding defense
     * @property fistPunch score of fist punch defense
     * @property legKick score of leg kick defense
     * @property knifeLock score of knife defense
     * @property weaponLock score of defense with hapkido weapon
     */
    class Adult(
        wristHold: Float = 0.1f,
        clothesHold: Float = 0.1f,
        fistPunch: Float = 0.1f,
        legKick: Float = 0.1f,
        knifeLock: Float = 0.1f,
        weaponLock: Float = 0.1f
    ): Junior(
        wristHold,
        clothesHold,
        fistPunch,
        legKick
    ){
        init {
            requireCriterionScore(knifeLock)
            requireCriterionScore(weaponLock)
        }
        
        var knifeLock by criterionScoreState(knifeLock)
        var weaponLock by criterionScoreState(weaponLock)

        override fun toString(): String {
            return super.toString() +
                    "knifeLock: $knifeLock,\n" +
                    "weaponLock: $weaponLock"
        }

        override fun getTotalScore(): Float {
            return super.getTotalScore() +
                    round((knifeLock + weaponLock) * 10) / 10f
        }
    }

    /**
     * [TechniqueCriteria] implementation for freestyle group discipline
     * @property offenseDefense offense & defense techniques
     * @property itemsBreaking breaking planks & other stuff
     * @property legKicks leg-kick techniques
     * @property weaponSkills weapon usage
     * @property dynamicMovement dynamics & movement of the performance
     * @property acrobatics acrobatic elements
     */
    class Group(
        offenseDefense: Float = 0.1f,
        itemsBreaking: Float = 0.1f,
        legKicks: Float = 0.1f,
        weaponSkills: Float = 0.1f,
        dynamicMovement: Float = 0.1f,
        acrobatics: Float = 0.1f
    ): TechniqueCriteria() {
        init {
            requireCriterionScore(offenseDefense)
            requireCriterionScore(itemsBreaking)
            requireCriterionScore(legKicks)
            requireCriterionScore(weaponSkills)
            requireCriterionScore(dynamicMovement)
            requireCriterionScore(acrobatics)
        }
        
        var offenseDefense by criterionScoreState(offenseDefense)
        var itemsBreaking by criterionScoreState(itemsBreaking)
        var legKicks by criterionScoreState(legKicks)
        var weaponSkills by criterionScoreState(weaponSkills)
        var dynamicMovement by criterionScoreState(dynamicMovement)
        var acrobatics by criterionScoreState(acrobatics)

        override fun toString(): String {
            return "offenseDefense: $offenseDefense,\n" +
                    "itemsBreaking: $itemsBreaking,\n" +
                    "legKicks: $legKicks,\n" +
                    "weaponUse: $weaponSkills,\n" +
                    "dynamicMovement: $dynamicMovement,\n" +
                    "acrobatics: $acrobatics"
        }
        
        override fun getTotalScore(): Float {
            return round(
                (offenseDefense + itemsBreaking + legKicks + 
                        weaponSkills + dynamicMovement + acrobatics) * 10
            ) / 10f
        }

    }

    /**
     * [TechniqueCriteria] implementation for freestyle with weapon
     * @property weaponTechniques techniques with weapon
     * @property jumpKicks leg kicks in air
     * @property rotateKicks leg kicks with rotation
     * @property weaponManipulation effective weapon usage
     * @property movement competitors movement during performance
     * @property acrobatics acrobatic elements
     */
    class Weapon(
        weaponTechniques: Float = 0.1f,
        jumpKicks: Float = 0.1f,
        rotateKicks: Float = 0.1f,
        weaponManipulation: Float = 0.1f,
        movement: Float = 0.1f,
        acrobatics: Float = 0.1f
    ): TechniqueCriteria() {
        init {
            requireCriterionScore(weaponTechniques)
            requireCriterionScore(jumpKicks)
            requireCriterionScore(rotateKicks)
            requireCriterionScore(weaponManipulation)
            requireCriterionScore(movement)
            requireCriterionScore(acrobatics)
        }
        
        var weaponTechniques by criterionScoreState(weaponTechniques)
        var jumpKicks by criterionScoreState(jumpKicks)
        var rotateKicks by criterionScoreState(rotateKicks)
        var weaponManipulation by criterionScoreState(weaponManipulation)
        var movement by criterionScoreState(movement)
        var acrobatics by criterionScoreState(acrobatics)

        override fun toString(): String {
            return "weaponTechniques: $weaponTechniques,\n" +
                    "jumpKicks: $jumpKicks,\n" +
                    "rotateKicks: $rotateKicks,\n" +
                    "weaponManipulation: $weaponManipulation,\n" +
                    "movement: $movement,\n" +
                    "acrobatics: $acrobatics"
        }
        
        override fun getTotalScore(): Float {
            return round(
                (weaponTechniques + jumpKicks + rotateKicks + 
                        weaponManipulation + movement + acrobatics) * 10
            ) / 10f
        }
    }
}
