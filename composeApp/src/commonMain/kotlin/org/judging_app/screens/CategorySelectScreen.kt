package org.judging_app.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.back_icon
import org.judging_app.State
import org.judging_app.enums.Categories
import org.judging_app.enums.Colors
import org.judging_app.enums.Routes
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles
import org.judging_app.ui.button.clickWithTransition

/**
 * Screen of category select
 */
object CategorySelectScreen : Screen {
    @Composable
    override fun load() {
        State.currentCategory = null
        State.currentRating = null
        val goBackOnclick = remember { {
            clickWithTransition(Routes.BACK)
        } }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight(0.15f)
                    .fillMaxWidth()
            ) {
                ButtonComponent(
                    style = ButtonStyles.Icon,
                    iconSrc = Res.drawable.back_icon,
                    onclick = goBackOnclick,
                    modifier = Modifier
                        .fillMaxHeight()
                )
                Spacer(Modifier.weight(0.8f))
                Text(
                    text = Localization.getString("category_title"),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = Localization.getString(State.currentDiscipline!!.value),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(30.dp))

            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Colors.SECONDARY.color),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier
                        .fillMaxHeight(0.8f)
                        .wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val categories = Categories.entries
                    for (i in categories.indices step 2) {
                        val first = categories[i]
                        val second = if (i + 1 < categories.size)
                            categories[i + 1] else null
                        val firstOnclick = {
                            State.currentCategory = first
                            clickWithTransition(
                                Routes.valueOf(
                                    "${State.currentDiscipline!!
                                        .value.split("_")[1].uppercase()}_MODE"
                                )
                            )
                        }
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ButtonComponent(
                                text = Localization.getString(first.value)
                                    .uppercase(),
                                onclick = firstOnclick,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.7f)
                                    .fillMaxWidth(0.9f)
                            )
                            if (second != null) {
                                val secondOnclick = {
                                    State.currentCategory = second
                                    clickWithTransition(
                                        Routes.valueOf(
                                            "${State.currentDiscipline!!
                                                .value.split("_")[1].uppercase()}_MODE"
                                        )
                                    )
                                }
                                Spacer(Modifier.weight(0.2f))
                                ButtonComponent(
                                    text = Localization.getString(second.value)
                                        .uppercase(),
                                    onclick = secondOnclick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(0.7f)
                                        .fillMaxWidth(0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}