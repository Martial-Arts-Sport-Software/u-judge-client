package org.mass.ui.rating

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mass.State
import org.mass.entities.TechniqueRating
import org.mass.locale.Localization
import org.mass.rating.TechnicalAvailability
import org.mass.rating.TechnicalSendOutcome
import org.mass.rating.TechnicalVector
import org.mass.ui.button.ButtonComponent
import org.mass.ui.button.ButtonStyles

/**
 * `Send` of a technical rating (server ADR-011 §7): enabled only for a seated judge of the running server performance of
 * this discipline (`CLI-048`), confirmed in a dialog that says the score cannot be changed afterwards (`CLI-049`), and
 * followed by a text status for pending, accepted, rejected and unavailable that does not rely on colour.
 */
@Composable
fun TechnicalSendComponent(rating: TechniqueRating, modifier: Modifier = Modifier) {
    val discipline = State.currentDiscipline ?: return
    val sends = State.technicalSends
    val vector = TechnicalVector.of(rating)
    val availability = sends.availability(discipline, vector)
    var confirming by remember { mutableStateOf(false) }
    val status = status(availability, sends.latestOutcome, State.session.technical?.sessionId, sends.sentTotalTenths())

    Column(modifier) {
        ButtonComponent(
            modifier = Modifier.weight(1f).semantics { contentDescription = status },
            style = ButtonStyles.Secondary,
            text = Localization.getString("hosinsool-result-send-btn"),
            onclick = { confirming = true },
            enabled = availability == TechnicalAvailability.AVAILABLE
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(Localization.getString("technical_send_confirm_title")) },
            text = {
                Text(
                    Localization.getString("technical_send_confirm_text")
                        .replace("%s", TechnicalVector.format(vector.totalTenths))
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    sends.send(discipline, vector)
                }) { Text(Localization.getString("technical_send_confirm")) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(Localization.getString("technical_send_back")) }
            }
        )
    }
}

private fun status(
    availability: TechnicalAvailability,
    outcome: TechnicalSendOutcome?,
    sessionId: String?,
    sentTotalTenths: Int?
): String {
    val own = outcome?.takeIf { it.sessionId == sessionId }
    return when {
        own is TechnicalSendOutcome.Pending -> Localization.getString("technical_send_pending")
        sentTotalTenths != null -> Localization.getString("technical_send_accepted").replace("%s", TechnicalVector.format(sentTotalTenths))
        own is TechnicalSendOutcome.Rejected -> Localization.getString("technical_send_rejected_${own.code}")
            .takeUnless { it.startsWith("technical_send_rejected_") }
            ?: Localization.getString("technical_send_rejected").replace("%s", own.code)
        else -> Localization.getString("technical_send_${availability.name.lowercase()}")
    }
}
