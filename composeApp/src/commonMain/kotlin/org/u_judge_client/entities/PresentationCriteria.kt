package org.u_judge_client.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.round

/**
 * rating's score for presentation, has:
 * - [PresentationCriteria.Hosinsool]
 */
sealed class PresentationCriteria {
    /**
     * fun to get total score for all presentation criteria
     * @return total sum
     */
    abstract fun getTotalScore(): Float

    /**
     * [PresentationCriteria] implementation for hosinsool
     * @property realism realism of techniques
     * @property power techniques power & speed
     * @property balance balance of techniques, fall techniques
     * @property harmony harmony & timing of the whole performance
     */
    class Hosinsool(
        realism: Float = 0.1f,
        power: Float = 0.1f,
        balance: Float = 0.1f,
        harmony: Float = 0.1f
    ): PresentationCriteria() {
        init {
            require(realism in 0.1f..1.0f)
            require(power in 0.1f..1.0f)
            require(balance in 0.1f..1.0f)
            require(harmony in 0.1f..1.0f)
        }

        var realism by mutableStateOf(realism)
        var power by mutableStateOf(power)
        var balance by mutableStateOf(balance)
        var harmony by mutableStateOf(harmony)

        override fun toString(): String {
            return "realism: $realism,\n" +
                    "power: $power,\n" +
                    "balance: $balance,\n" +
                    "harmony: $harmony"
        }

        override fun getTotalScore(): Float {
            return round((realism + power + balance + harmony) * 10f) / 10f
        }
    }

    /**
     * [PresentationCriteria] implementation for hosinsool
     * @property creativity realism & creativity of the performance
     * @property power speed & power of techniques, fight spirit
     * @property balance balance, fall techniques, acrobatic elements
     * @property choreography musical accompaniment, choreography of the performance
     */
    open class FreestylePair(
        creativity: Float = 0.1f,
        power: Float = 0.1f,
        balance: Float = 0.1f,
        choreography: Float = 0.1f
    ): PresentationCriteria() {
        init {
            require(creativity in 0.1f..1f)
            require(power in 0.1f..1f)
            require(balance in 0.1f..1f)
            require(choreography in 0.1f..1f)
        }

        var creativity by mutableStateOf(creativity)
        var power by mutableStateOf(power)
        var balance by mutableStateOf(balance)
        var choreography by mutableStateOf(choreography)

        override fun toString(): String {
            return "creativity: $creativity,\n" +
                    "power: $power,\n" +
                    "balance: $balance,\n" +
                    "choreography: $choreography"
        }

        override fun getTotalScore(): Float {
            return round(
                (creativity + power + balance + choreography) * 10
            ) / 10f
        }
    }

    class FreestyleGroup(
        creativity: Float = 0.1f,
        power: Float = 0.1f,
        balance: Float = 0.1f,
        choreography: Float = 0.1f
    ) : FreestylePair(
        creativity,
        power,
        balance,
        choreography
    )

    class FreestyleWeapon(
        creativity: Float = 0.1f,
        power: Float = 0.1f,
        balance: Float = 0.1f,
        choreography: Float = 0.1f
    ) : FreestylePair(
        creativity,
        power,
        balance,
        choreography
    )
}