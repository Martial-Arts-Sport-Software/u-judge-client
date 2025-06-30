package org.judging_app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.judging_app.State
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
                    .background(Color(0xFF7C45E2))
            )
        }
    }
}