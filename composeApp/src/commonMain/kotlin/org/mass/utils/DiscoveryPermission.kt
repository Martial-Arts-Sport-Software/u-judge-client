package org.mass.utils

import androidx.compose.runtime.Composable

/**
 * Asks for the platform permission local-network discovery needs (Android 13+ `NEARBY_WIFI_DEVICES`) and then calls
 * [onReady]. Discovery still starts after a denial: the platforms currently only warn without it, and the manual tab
 * remains available either way.
 */
@Composable
expect fun DiscoveryPermissionEffect(key: Any?, onReady: () -> Unit)
