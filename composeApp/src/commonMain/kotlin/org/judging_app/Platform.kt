package org.judging_app

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun getLocale(): String

@Composable
expect fun getTypography(): Typography

@Composable
expect fun PDFViewer(
    modifier: Modifier = Modifier
)