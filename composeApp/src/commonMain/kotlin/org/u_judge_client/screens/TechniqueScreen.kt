package org.u_judge_client.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.belt
import u_judge_client.composeapp.generated.resources.clothes
import u_judge_client.composeapp.generated.resources.fist
import u_judge_client.composeapp.generated.resources.foot
import u_judge_client.composeapp.generated.resources.knife
import u_judge_client.composeapp.generated.resources.wrist
import org.u_judge_client.State
import org.u_judge_client.entities.TechniqueCriteria
import org.u_judge_client.entities.TechniqueRating
import org.u_judge_client.enums.Categories
import org.u_judge_client.enums.Disciplines
import org.u_judge_client.locale.Localization
import org.u_judge_client.ui.input.RangeInputComponent

/**
 * Interface of all technique screens - hosinsool, freestyle
 */
interface TechniqueScreen : Screen {
    var currentDisplay: DISPLAY
    var nextDisplay: DISPLAY
    var showInformation: Boolean
    /**
     * Possible display of all technique discipline
     * @property TECHNIQUE - display of technique criteria
     * @property PRESENTATION - display of presentation criteria
     * @property RESULT - display with score sum for technique and presentation separately and total score sum
     */
    enum class DISPLAY {
        TECHNIQUE,
        PRESENTATION,
        RESULT
    }

    /**
     * Render header for all technique screens
     */
    @Composable
    fun TechniqueScreenHeader() {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Localization.getString(
                    "discipline_${State.currentDiscipline.toString().lowercase()}"
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = when(HosinsoolModeScreen.currentDisplay) {
                    DISPLAY.TECHNIQUE -> Localization.getString(
                        "hosinsool_technique"
                    )
                    DISPLAY.PRESENTATION -> Localization.getString(
                        "hosinsool_presentation"
                    )
                    DISPLAY.RESULT -> Localization.getString(
                        "hosinsool_result"
                    )
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (State.currentCategory?.value
                    == Categories.ADULTS.value)
                    Localization.getString("category_adults")
                else Localization.getString("category_juniors"),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = Localization.getString("kerugi_judge") +
                        " - ${
                            State.judgeSurname.ifEmpty {
                                Localization.getString("kerugi_judge_empty")
                            }}",
                style = MaterialTheme.typography.titleSmall
            )
        }
        Spacer(Modifier.height(10.dp))
    }

    /**
     * Renders display for technique criteria in all [TechniqueScreen] implementations.
     *
     * Only for [Disciplines.HOSINSOOL] and [Disciplines.FREESTYLE_PAIR]
     */
    @Composable
    fun DefaultTechniqueDisplay() {
        if (State.currentRating != null) {
            val rating by remember { mutableStateOf(State.currentRating as TechniqueRating) }
            RangeInputComponent(
                currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                    .wristHold,
                onValueChange = { value ->
                    (rating.techniqueCriteria as TechniqueCriteria.Junior).wristHold = value
                },
                icon = Res.drawable.wrist
            )
            RangeInputComponent(
                currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                    .clothesHold,
                onValueChange = { value ->
                    (rating.techniqueCriteria as TechniqueCriteria.Junior).clothesHold = value
                },
                icon = Res.drawable.clothes
            )
            RangeInputComponent(
                currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                    .fistPunch,
                onValueChange = { value ->
                    (rating.techniqueCriteria as TechniqueCriteria.Junior).fistPunch = value
                },
                icon = Res.drawable.fist
            )
            RangeInputComponent(
                currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                    .legKick,
                onValueChange = { value ->
                    (rating.techniqueCriteria as TechniqueCriteria.Junior).legKick = value
                },
                icon = Res.drawable.foot
            )
            if (State.currentCategory == Categories.ADULTS) {
                RangeInputComponent(
                    currentValue = (rating.techniqueCriteria as TechniqueCriteria.Adult)
                        .knifeLock,
                    onValueChange = { value ->
                        (rating.techniqueCriteria as TechniqueCriteria.Adult).knifeLock = value
                    },
                    icon = Res.drawable.knife
                )
                RangeInputComponent(
                    currentValue = (rating.techniqueCriteria as TechniqueCriteria.Adult)
                        .weaponLock,
                    onValueChange = { value ->
                        (rating.techniqueCriteria as TechniqueCriteria.Adult).weaponLock = value
                    },
                    icon = Res.drawable.belt
                )
            }
        }
    }
}