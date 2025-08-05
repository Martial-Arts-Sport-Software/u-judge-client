package org.judging_app.entities

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
     * [TechniqueCriteria] implementation for junior category
     * @property wristHold - criterion, responsible for score of wrist holding defense
     * @property clothesHold - criterion, responsible for score of clothes holding defense
     * @property fistPunch - criterion, responsible for score of fist punch defense
     * @property legKick - criterion, responsible for score of leg kick defense
     */
    open class Junior(
        wristHold: Float = 0.1f,
        clothesHold: Float = 0.1f,
        fistPunch: Float = 0.1f,
        legKick: Float = 0.1f,
    ): TechniqueCriteria() {
        var wristHold by mutableStateOf(wristHold)
        var clothesHold by mutableStateOf(clothesHold)
        var fistPunch by mutableStateOf(fistPunch)
        var legKick by mutableStateOf(legKick)

        init {
            require(wristHold in 0.1f..1f)
            require(clothesHold in 0.1f..1f)
            require(fistPunch in 0.1f..1f)
            require(legKick in 0.1f..1f)
        }

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
     * [TechniqueCriteria] implementation for junior category
     * @property wristHold - criterion, responsible for score of wrist holding defense
     * @property clothesHold - criterion, responsible for score of clothes holding defense
     * @property fistPunch - criterion, responsible for score of fist punch defense
     * @property legKick - criterion, responsible for score of leg kick defense
     * @property knifeLock - criterion, responsible for score of knife defense
     * @property weaponLock - criterion, responsible for score of defense with hapkido weapon
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
        var knifeLock by mutableStateOf(knifeLock)
        var weaponLock by mutableStateOf(weaponLock)

        init {
            require(knifeLock in 0.1f..1f)
            require(weaponLock in 0.1f..1f)
        }

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
}