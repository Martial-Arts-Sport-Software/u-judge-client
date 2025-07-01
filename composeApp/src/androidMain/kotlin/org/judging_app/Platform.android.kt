package org.judging_app

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import judging_app_client.composeapp.generated.resources.Montserrat
import judging_app_client.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font
import java.util.Locale
import kotlin.system.exitProcess

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
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
        titleSmall = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 3.5.em,
            fontWeight = FontWeight.Bold,
            color = Color.White
        ),
        bodyLarge = TextStyle(
            fontFamily = montserratVariable,
            fontSize = 3.em
        )
    )
}