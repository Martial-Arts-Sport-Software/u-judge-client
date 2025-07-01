package org.judging_app.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.club_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.ButtonComponent
import org.judging_app.ui.Styles

object EntryScreen: Screen {
    @Composable
    override fun load(controller: NavController) {
        LaunchedEffect(State.isAnimating) {
            if (State.isAnimating.value) {
                delay(400)
                State.isAnimating.value = false
            }
        }

        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFEFD4FF)),
        ) {
            Row {
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(
                            topStart = 15.dp,
                            bottomStart = 15.dp
                        ))
                        .background(Color(0xFF7C45E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            Localization.getString("entry_club_name"),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier.fillMaxHeight(0.5f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.club_logo),
                                contentDescription = null,
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier.fillMaxHeight()
                            )
                            Spacer(Modifier.width(20.dp))
                            Text(
                                Localization.getString("entry_club_description"),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val loginBtnClicked = remember { mutableStateOf(false) }
                    val offlineBtnClicked = remember { mutableStateOf(false) }

                    Spacer(Modifier.weight(1f))

                    val loginOnClick = remember { {
                        if (loginBtnClicked.value) State.currentLocale.value = "en"
                        else State.currentLocale.value = "ru"

                        loginBtnClicked.value = !loginBtnClicked.value
                    } }
                    val offlineOnClick = remember { {
                        State.isAnimating.value = true
                        controller.navigate(State.Routes.DISCIPLINE_MODE.path)
                    } }

                    ButtonComponent(
                        Localization.getString("entry_login"),
                        onclick = loginOnClick
                    )
                    ButtonComponent(
                        Localization.getString("entry_offline"),
                        onclick = offlineOnClick,
                        style = Styles.Secondary
                    )

                    Spacer(Modifier.weight(1f))

                    Row(
                        Modifier.fillMaxHeight(0.2f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,

                    ) {
                        ButtonComponent(
                            "Русский",
                            Styles.Plain,
                            onclick = { State.currentLocale.value = "ru" },
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider(
                            color = Color(0xFF7C45E2),
                            thickness = 2.dp,
                            modifier = Modifier
                                .fillMaxHeight(0.5f)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        ButtonComponent(
                            "English",
                            Styles.Plain,
                            onclick = { State.currentLocale.value = "en" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}