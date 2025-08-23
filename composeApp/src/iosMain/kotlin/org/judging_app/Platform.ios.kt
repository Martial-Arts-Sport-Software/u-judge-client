package org.judging_app

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import judging_app_client.composeapp.generated.resources.Montserrat
import judging_app_client.composeapp.generated.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.Font
import org.judging_app.enums.Colors
import org.judging_app.ui.popup.Popup
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSLocale
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIView

actual fun getLocale(): String = NSLocale.currentLocale.languageCode

@Composable
actual fun PDFViewer(
    filename: String,
    modifier: Modifier
) {
    val transition = updateTransition(State.currentPopupMode)
    var pdfData by remember { mutableStateOf<ByteArray?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(filename) {
        pdfData = Res.readBytes(filename)
    }

    LaunchedEffect(transition.currentState) {
        if (transition.currentState == Popup.Modes.INFORMATION) {
            delay(300)
            isLoaded = true
        } else isLoaded = false
    }

    pdfData?.let { data ->
        val pdfView = createPDFView(data.toNSDataByteArray())
        if (isLoaded) {
            UIKitView(
                modifier = modifier,
                factory = { pdfView }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun createPDFView(pdfData: NSData): UIView {
    val pdfView = PDFView(frame = CGRectMake(0.0, 0.0, 300.0, 400.0))
    pdfView.document = PDFDocument(pdfData)
    pdfView.pageShadowsEnabled = false
    pdfView.pageBreakMargins = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)
    pdfView.autoScales = true
    pdfView.backgroundColor = UIColor.whiteColor
    pdfView.opaque = true
    return pdfView
}

@OptIn(kotlinx.cinterop.BetaInteropApi::class, ExperimentalForeignApi::class)
fun ByteArray.toNSDataByteArray(): NSData {
    return this.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = this.size.toULong()
        )
    }
}

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
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 40.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 40.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        ),
        bodyLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 12.sp,
            color = Color.White
        ),
        bodyMedium = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 9.sp,
            color = Color.White,
        ),
        labelLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 15.sp,
        ),
        displayLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 20.sp,
            color = Colors.PRIMARY.color
        )
    )
}