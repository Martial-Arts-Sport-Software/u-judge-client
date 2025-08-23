package org.judging_app.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.cross_icon
import org.judging_app.PDFViewer
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles

/**
 * Renders information popup
 */
@Composable
fun InformationPopupComponent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight(0.2f)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 15.dp),
                text = Localization.getString("hosinsool-info-title"),
                style = MaterialTheme.typography.displayLarge
            )
        }
        ButtonComponent(
            modifier = Modifier
                .fillMaxHeight(0.2f)
                .align(Alignment.TopEnd),
            style = ButtonStyles.Icon,
            iconSrc = Res.drawable.cross_icon,
            onclick = {
                State.currentPopupMode = Popup.Modes.NONE
            }
        )
        PDFViewer(
            Modifier.align(Alignment.BottomCenter)
        )
    }
}
