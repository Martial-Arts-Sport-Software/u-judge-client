package org.judging_app.entities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.round

sealed class PresentationCriteria {
    abstract fun getTotalScore(): Float

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