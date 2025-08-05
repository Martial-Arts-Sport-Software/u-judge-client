package org.judging_app.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.round

/**
 * rating's score for presentation, has:
 * - [PresentationCriteria.HosinsoolPresentationCriteria]
 */
sealed class PresentationCriteria {
    /**
     * fun to get total score for all presentation criteria
     * @return total sum
     */
    abstract fun getTotalScore(): Float

    /**
     * [PresentationCriteria] implementation for hosinsool
     * @property realism - first criterion
     * @property realism - second criterion
     * @property realism - third criterion
     * @property realism - fourth criterion
     */
    class HosinsoolPresentationCriteria(
        realism: Float,
        power: Float,
        balance: Float,
        harmony: Float
    ): PresentationCriteria() {
        var realism by mutableStateOf(realism)
        var power by mutableStateOf(power)
        var balance by mutableStateOf(balance)
        var harmony by mutableStateOf(harmony)
        init {
            require(realism in 0.1f..1.0f)
            require(power in 0.1f..1.0f)
            require(balance in 0.1f..1.0f)
            require(harmony in 0.1f..1.0f)
        }

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
}