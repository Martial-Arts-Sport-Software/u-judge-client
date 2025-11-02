package org.u_judge_client.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.round

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
            require(wristHold in 0.1f..1f)
            require(clothesHold in 0.1f..1f)
            require(fistPunch in 0.1f..1f)
            require(legKick in 0.1f..1f)
        }
        
        var wristHold by mutableStateOf(wristHold)
        var clothesHold by mutableStateOf(clothesHold)
        var fistPunch by mutableStateOf(fistPunch)
        var legKick by mutableStateOf(legKick)

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
            require(knifeLock in 0.1f..1f)
            require(weaponLock in 0.1f..1f)
        }
        
        var knifeLock by mutableStateOf(knifeLock)
        var weaponLock by mutableStateOf(weaponLock)

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
            require(offenseDefense in 0.1f..1f)
            require(itemsBreaking in 0.1f..1f)
            require(legKicks in 0.1f..1f)
            require(weaponSkills in 0.1f..1f)
            require(dynamicMovement in 0.1f..1f)
            require(acrobatics in 0.1f..1f)
        }
        
        var offenseDefense by mutableStateOf(offenseDefense)
        var itemsBreaking by mutableStateOf(itemsBreaking)
        var legKicks by mutableStateOf(legKicks)
        var weaponSkills by mutableStateOf(weaponSkills)
        var dynamicMovement by mutableStateOf(dynamicMovement)
        var acrobatics by mutableStateOf(acrobatics)

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
            require(weaponTechniques in 0.1f..1f)
            require(jumpKicks in 0.1f..1f)
            require(rotateKicks in 0.1f..1f)
            require(weaponManipulation in 0.1f..1f)
            require(movement in 0.1f..1f)
            require(acrobatics in 0.1f..1f)
        }
        
        var weaponTechniques by mutableStateOf(weaponTechniques)
        var jumpKicks by mutableStateOf(jumpKicks)
        var rotateKicks by mutableStateOf(rotateKicks)
        var weaponManipulation by mutableStateOf(weaponManipulation)
        var movement by mutableStateOf(movement)
        var acrobatics by mutableStateOf(acrobatics)

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