package org.judging_app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.app_background
import judging_app_client.composeapp.generated.resources.club_logo
import org.jetbrains.compose.resources.painterResource
import org.judging_app.locale.Localization
import org.judging_app.ui.ButtonComponent

object EntryScreen: Screen {
    @Composable
    override fun load(controller: NavController) {
        Box(
            Modifier
                .width(percentWidth(0.85f))
                .height(percentHeight(0.8f))
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFEFD4FF)),
        ) {
            Box(
                Modifier
                    .width(percentWidth(0.5f))
                    .height(percentHeight(0.8f))
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.club_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(percentWidth(0.1f))
                        )
                        Spacer(Modifier.width(20.dp))
                        Text(
                            Localization.getString("entry_club_description"),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}