package org.judging_app.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.club_logo
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles
import org.judging_app.ui.button.clickWithTransition
import org.judging_app.ui.input.TextInputComponent

object EntryScreen: Screen {
    @Composable
    override fun load() {
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
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

                    Spacer(Modifier.weight(0.8f))

                    TextInputComponent(
                        Localization.getString("entry_server_address"),
                        inputValue = State.serverAddress,
                        onChange = { inputValue ->  
                            State.serverAddress = inputValue
                        }
                    )
                    TextInputComponent(
                        Localization.getString("entry_judge_surname"),
                        inputValue = State.judgeSurname,
                        onChange = { inputValue ->
                            State.judgeSurname = inputValue
                        }
                    )

                    val loginOnClick = remember { {
                        State.isOffline.value = false
                        clickWithTransition(State.Routes.DISCIPLINE_SELECT)
                    } }
                    val offlineOnClick = remember { {
                        State.isOffline.value = true
                        clickWithTransition(State.Routes.DISCIPLINE_SELECT)
                    } }

                    Spacer(Modifier.weight(0.3f))

                    Row(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(0.2f)
                    ) {
                        ButtonComponent(
                            text = Localization.getString("entry_login"),
                            onclick = loginOnClick,
                            modifier = Modifier.weight(1f),
                            enabled = State.serverAddress.isNotBlank()
                                    && State.judgeSurname.isNotBlank()
                        )

                        Spacer(Modifier.width(10.dp))

                        ButtonComponent(
                            Localization.getString("entry_offline"),
                            onclick = offlineOnClick,
                            style = ButtonStyles.Secondary,
                            modifier = Modifier.weight(1f),
                            enabled = State.judgeSurname.isNotBlank()
                        )
                    }
                    Spacer(Modifier.weight(0.6f))
                    Row(
                        Modifier
                            .padding(bottom = 5.dp)
                            .fillMaxHeight(0.3f)
                            .fillMaxWidth(0.8f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ButtonComponent(
                            "Русский",
                            ButtonStyles.Plain,
                            onclick = { State.currentLocale.value = "ru" },
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider(
                            color = Color(0xFF7C45E2),
                            thickness = 1.5.dp,
                            modifier = Modifier
                                .fillMaxHeight(0.4f)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        ButtonComponent(
                            "English",
                            ButtonStyles.Plain,
                            onclick = { State.currentLocale.value = "en" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}