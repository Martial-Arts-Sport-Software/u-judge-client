package org.u_judge_client

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun getContext(): Any?

expect fun vibrate(context: Any?)

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