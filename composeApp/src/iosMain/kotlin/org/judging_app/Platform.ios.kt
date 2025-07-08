package org.judging_app

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import judging_app_client.composeapp.generated.resources.Montserrat
import judging_app_client.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.UIKit.UIDevice
import platform.posix.exit

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
actual fun getLocale(): String = NSLocale.currentLocale.languageCode
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
            fontSize = 9.sp
        ),
        labelLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 15.sp,
        ),
        displayLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 25.sp,
            color = State.Colors.PRIMARY.color
        )
    )
}