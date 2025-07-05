package org.judging_app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.back_icon
import judging_app_client.composeapp.generated.resources.club_logo
import kotlinx.coroutines.delay
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.Styles
import org.judging_app.ui.button.clickWithTransition

object DisciplineModeScreen: Screen {
    @Composable
    override fun load(controller: NavController) {
        val goBackOnclick = remember { {
            clickWithTransition(State.Routes.BACK)
        } }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight(0.15f)
                    .fillMaxWidth()
            ) {
                ButtonComponent(
                    style = Styles.Icon,
                    iconSrc = Res.drawable.back_icon,
                    onclick = goBackOnclick,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.2f)
                        .weight(0.1f)
                )
                Spacer(Modifier.weight(0.9f))
                Text(
                    text = Localization.getString("discipline_title"),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(State.Colors.SECONDARY.color),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .fillMaxHeight(0.9f)
                        .fillMaxWidth(0.9f),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val disciplines = State.Disciplines.entries
                    for (i in disciplines.indices step 2) {
                        val first = disciplines[i]
                        val second = if (i + 1 < disciplines.size)
                            disciplines[i + 1] else null
                        val firstOnclick = {
                            State.currentDiscipline = first
                            clickWithTransition(
                                State.Routes.CATEGORY_MODE
                            )
                        }
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ButtonComponent(
                                text = Localization.getString(first.value)
                                    .uppercase(),
                                onclick = firstOnclick,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.8f)
                            )
                            if (second != null) {
                                val secondOnclick = {
                                    State.currentDiscipline = second
                                    clickWithTransition(
                                        State.Routes.CATEGORY_MODE
                                    )
                                }
                                Spacer(Modifier.weight(0.1f))
                                ButtonComponent(
                                    text = Localization.getString(second.value)
                                        .uppercase(),
                                    onclick = secondOnclick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}