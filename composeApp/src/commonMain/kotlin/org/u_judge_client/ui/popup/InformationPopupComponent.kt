package org.u_judge_client.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.cross_icon
import org.u_judge_client.PDFViewer
import org.u_judge_client.State
import org.u_judge_client.locale.Localization
import org.u_judge_client.ui.button.ButtonComponent
import org.u_judge_client.ui.button.ButtonStyles

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
        PDFViewer(
            filename =
                "files/${State.currentDiscipline?.name?.lowercase()}" +
                    "/${State.currentLocale}.pdf",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(start = 15.dp, end = 15.dp, bottom = 10.dp)
                .align(Alignment.BottomCenter)
        )
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
    }
}
