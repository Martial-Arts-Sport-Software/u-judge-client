package org.mass.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.kerugi_chestplate
import u_judge_client.composeapp.generated.resources.kerugi_helmet
import org.mass.PairedServerSession
import org.mass.State
import org.mass.combat.KerugiParticipant
import org.mass.combat.KerugiTarget
import org.mass.enums.Colors
import org.mass.locale.Localization
import org.mass.session.Competitor
import org.mass.session.SessionSnapshot
import org.mass.ui.button.CombatButtonComponent
import org.mass.ui.button.CombatButtonPositions
import org.mass.ui.navbar.NavbarComponent
import org.mass.ui.navbar.NavbarStyles
import org.mass.ui.popup.Popup
import org.mass.ui.popup.SettingsPopupComponent
import org.mass.ui.popup.WarningPopupComponent

/**
 * Screen of kerugi discipline: the server bout (number, category, sides, state, score) and the four scoring controls,
 * enabled only for a seated judge of a running bout (server ADR-009 §10).
 */
object KerugiModeScreen : Screen {
    @Composable
    override fun Load() {
        Column(
            Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier.fillMaxHeight(0.15f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavbarComponent(
                    NavbarStyles.HORIZONTAL,
                    Modifier.weight(0.45f),
                )
                Spacer(Modifier.weight(0.25f))
                Text(
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(horizontal = 3.dp),
                    text = Localization.getString("kerugi_judge") +
                            " - ${State.judgeSurname.ifEmpty {
                                Localization.getString("kerugi_judge_empty")
                            }
                            }",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Right
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = State.currentPopupMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { target ->
                when (target) {
                    Popup.Modes.SETTINGS -> {
                        Column(
                            Modifier
                                .fillMaxHeight(0.95f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SettingsPopupComponent()
                        }
                    }
                    Popup.Modes.WARNING -> {
                        Column(
                            Modifier
                                .fillMaxHeight(0.95f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WarningPopupComponent()
                        }
                    }
                    else -> JudgePanel()
                }
            }
        }
    }

    @Composable
    private fun JudgePanel() {
        val commands = State.kerugiCommands
        val snapshot = State.session.snapshot
        val enabled = commands.isAvailable
        Column(
            Modifier
                .fillMaxHeight(0.95f)
                .fillMaxWidth()
        ) {
            BoutHeader(snapshot, Modifier.weight(0.2f))
            Spacer(Modifier.weight(0.02f))
            Row(Modifier.weight(0.32f)) {
                CombatButtonComponent(
                    position = CombatButtonPositions.LEFT,
                    color = Colors.BLUE,
                    icon = Res.drawable.kerugi_helmet,
                    semanticLabel = Localization.getString("combat_blue_head"),
                    onclick = { commands.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.weight(0.02f))
                CombatButtonComponent(
                    position = CombatButtonPositions.RIGHT,
                    color = Colors.RED,
                    icon = Res.drawable.kerugi_helmet,
                    semanticLabel = Localization.getString("combat_red_head"),
                    onclick = { commands.submit(KerugiParticipant.RED, KerugiTarget.HEAD) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.weight(0.02f))
            Row(Modifier.weight(0.32f)) {
                CombatButtonComponent(
                    position = CombatButtonPositions.LEFT,
                    color = Colors.BLUE,
                    icon = Res.drawable.kerugi_chestplate,
                    semanticLabel = Localization.getString("combat_blue_body"),
                    onclick = { commands.submit(KerugiParticipant.BLUE, KerugiTarget.BODY) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.weight(0.02f))
                CombatButtonComponent(
                    position = CombatButtonPositions.RIGHT,
                    color = Colors.RED,
                    icon = Res.drawable.kerugi_chestplate,
                    semanticLabel = Localization.getString("combat_red_body"),
                    onclick = { commands.submit(KerugiParticipant.RED, KerugiTarget.BODY) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
            val status = listOfNotNull(
                KerugiBoutPresentation.availability(commands.availability, PairedServerSession.sessionSync.failureCode),
                KerugiBoutPresentation.tapFeedback(commands.latestOutcome),
                KerugiBoutPresentation.attentionFeedback(commands.latestAttention),
                KerugiBoutPresentation.pendingCount(commands.pendingCount)
            ) + KerugiBoutPresentation.notices(snapshot)
            Text(
                text = status.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }

    /** Blue and red sides with their server labels and score around the bout details. */
    @Composable
    private fun BoutHeader(snapshot: SessionSnapshot?, modifier: Modifier) {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SideCard(snapshot, Competitor.BLUE, Colors.BLUE, Modifier.weight(1f))
            Text(
                text = KerugiBoutPresentation.details(snapshot),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.9f).padding(horizontal = 6.dp)
            )
            SideCard(snapshot, Competitor.RED, Colors.RED, Modifier.weight(1f))
        }
    }

    @Composable
    private fun SideCard(snapshot: SessionSnapshot?, competitor: Competitor, color: Colors, modifier: Modifier) {
        val side = when (competitor) {
            Competitor.BLUE -> snapshot?.blue
            Competitor.RED -> snapshot?.red
        }
        Column(
            modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(color.color.copy(alpha = 0.35f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if (competitor == Competitor.BLUE) Alignment.Start else Alignment.End
        ) {
            Text(
                text = KerugiBoutPresentation.sideName(competitor) + ": " + (side?.label ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = KerugiBoutPresentation.sideScore(snapshot, competitor),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
        }
    }
}
