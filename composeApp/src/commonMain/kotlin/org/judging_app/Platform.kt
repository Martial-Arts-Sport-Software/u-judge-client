package org.judging_app

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun getLocale(): String

@Composable
expect fun getTypography(): Typography

/**
 * Integrates built-in PDF Viewer
 * @param filename path to PDF-file
 * @param modifier [Modifier] applied to container
 */
@Composable
expect fun PDFViewer(
    filename: String,
    modifier: Modifier = Modifier
)