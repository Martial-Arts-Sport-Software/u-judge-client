package org.judging_app.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.round
import kotlin.math.roundToInt

sealed class TechniqueCriteria {
    abstract fun getTotalScore(): Float

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