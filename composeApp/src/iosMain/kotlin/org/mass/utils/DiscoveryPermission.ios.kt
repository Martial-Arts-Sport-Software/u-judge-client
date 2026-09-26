package org.mass.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** iOS asks for Local Network access itself on the first Bonjour browse. */
@Composable
actual fun DiscoveryPermissionEffect(key: Any?, onReady: () -> Unit) {
    LaunchedEffect(key) { onReady() }
}
