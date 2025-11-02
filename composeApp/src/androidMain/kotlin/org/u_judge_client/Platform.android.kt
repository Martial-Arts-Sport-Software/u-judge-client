package org.u_judge_client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import com.infomaniak.lib.pdfview.PDFView
import com.infomaniak.lib.pdfview.scroll.DefaultScrollHandle
import u_judge_client.composeapp.generated.resources.Montserrat
import u_judge_client.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font
import org.u_judge_client.enums.Colors
import java.io.File
import java.util.Locale

@Composable
actual fun PDFViewer(
    filename: String,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val pdfFile = remember {
        File(context.cacheDir, filename.substringAfterLast('/'))
    }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(filename) {
        isLoaded = false
        try {
            val bytes = Res.readBytes(filename)
            pdfFile.writeBytes(bytes)
            isLoaded = pdfFile.exists() && pdfFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            isLoaded = false
        }
    }

    when {
        !isLoaded -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Box(modifier) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    factory = { ctx ->
                        PDFView(ctx, null).apply {
                            fromFile(pdfFile)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .scrollHandle(DefaultScrollHandle(ctx))
                                .load()
                        }
                    }
                )
            }
        }
    }
}

actual fun getLocale(): String = Locale.getDefault().language

@Composable
actual fun getTypography(): Typography {
    val montserratVariable = FontFamily(
        Font(
            resource = Res.font.Montserrat,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal
        )
    )

    return Typography(
        titleLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 9.em,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 1.em,
        ),
        titleMedium = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 5.em,
            fontWeight = FontWeight.Bold,
            color = Color.White
        ),
        titleSmall = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 3.5.em,
            fontWeight = FontWeight.Bold,
            color = Color.White
        ),
        bodyLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 3.em,
            color = Color.White
        ),
        bodyMedium = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 2.em,
            color = Color.White
        ),
        labelLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 3.em,
        ),
        displayLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 5.em,
            color = Colors.PRIMARY.color
        )
    )
}
