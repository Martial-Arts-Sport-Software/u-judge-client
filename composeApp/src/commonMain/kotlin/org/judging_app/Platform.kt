package org.judging_app

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getLocale(): String
@Composable
expect fun getTypography(): Typography