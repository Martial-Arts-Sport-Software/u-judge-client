package org.judging_app

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import judging_app_client.composeapp.generated.resources.Res

expect fun getLocale(): String

@Composable
expect fun getTypography(): Typography

@Composable
fun readFile(filename: String): List<String> {
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    val resourcePath = "files/$filename"

    LaunchedEffect(resourcePath) {
        try {
            val bytes = Res.readBytes(resourcePath)
            val content = bytes.decodeToString()
            lines = content.lines()
        } catch (e: Exception) {
            lines = listOf("Error loading $filename")
        }
    }

    return lines
}