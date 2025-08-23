package org.judging_app

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import judging_app_client.composeapp.generated.resources.Montserrat
import judging_app_client.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font
import org.judging_app.enums.Colors
import java.io.File
import java.util.Locale
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun loadPdfRenderer(filepath: String): PdfRenderer? {
    val context = LocalContext.current
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    LaunchedEffect(filepath) {
        val pdfBytes = Res.readBytes(filepath)
        val tempFile = File(context.cacheDir, filepath.substringAfterLast('/'))
        tempFile.writeBytes(pdfBytes)
        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
    }
    return renderer
}

@Composable
actual fun PDFViewer(
    modifier: Modifier
) {
    val filepath = "files/${State.currentDiscipline?.name?.lowercase()}" +
            "/${State.currentLocale}.pdf"
    val pdfRenderer = loadPdfRenderer(filepath)
    pdfRenderer?.let {
        val coroutineScope = rememberCoroutineScope()
        var textColumnSize by remember { mutableStateOf(IntSize.Zero) }
        var scrollbarSize by remember { mutableStateOf(IntSize.Zero) }
        var thumbSize by remember { mutableStateOf(IntSize.Zero) }

        val scrollState = rememberScrollState()
        val scrollFraction = if (scrollState.maxValue > 0)
            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        else 0f
        val pageCount = pdfRenderer.pageCount
        val renderedPages = remember(pageCount) {
            Array(pageCount) { mutableStateOf<ImageBitmap?>(null) }
        }
        Row(
            modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(start = 15.dp, end = 15.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .verticalScroll(scrollState)
                    .fillMaxHeight()
                    .fillMaxWidth(0.98f)
                    .onSizeChanged { textColumnSize = it }
            ) {
                for (i in 0 until pageCount) {
                    val imageBitmap = renderedPages[i].value
                    if (imageBitmap == null) {
                        LaunchedEffect(i) {
                            val page = pdfRenderer.openPage(i)
                            val bitmap = createBitmap(page.width, page.height)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            renderedPages[i].value = bitmap.asImageBitmap()
                        }
                        Text("Page loading error...")
                    } else {
                        Image(
                            modifier = Modifier.fillMaxWidth(),
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
            Box(
                Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .onSizeChanged { scrollbarSize = it }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1 / 3f)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (scrollFraction * (scrollbarSize.height - thumbSize.height)).roundToInt()
                            )
                        }
                        .background(Colors.PRIMARY.color, RoundedCornerShape(50))
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                val maxDrag = textColumnSize.height - scrollbarSize.height
                                val adjustedDelta = delta * 40f
                                val newScrollPosition = (scrollState.value + adjustedDelta / maxDrag * scrollState.maxValue)
                                    .coerceIn(0f, scrollState.maxValue.toFloat())

                                coroutineScope.launch {
                                    scrollState.scrollTo(newScrollPosition.toInt())
                                }
                            }
                        )
                        .onSizeChanged { thumbSize = it }
                )
            }
        }
    } ?: run { Text("Loading error...") }
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