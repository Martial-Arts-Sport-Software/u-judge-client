package org.mass.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders text input component
 * @param labelText - label text above input
 * @param inputValue - initial value of text input
 * @param onChange - callback, that is called on component's value change
 * @param enabled - is input available to fill or not, true by default
 * @param modifier - width and placement; by default 80% of the available width
 * @param fieldHeight - fixed height of the field with the text centered in it, to line it up with a button; null keeps
 * the height of the text and padding
 * @param bottomSpacing - space under the field
 */
@Composable
fun TextInputComponent(
    labelText: String? = null,
    inputValue: String = "",
    onChange: (inputValue: String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(0.8f),
    fieldHeight: Dp? = null,
    bottomSpacing: Dp = 10.dp,
) {
    Column(
        modifier
            .padding(bottom = bottomSpacing)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        if (labelText != null) {
            Text(
                labelText,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(5.dp))
        }
        val input = remember { mutableStateOf(inputValue) }
        BasicTextField(
            value = input.value,
            onValueChange = { it: String ->
                input.value = it
                onChange(input.value)
            },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier
                .background(
                    Color.White,
                    shape = RoundedCornerShape(5.dp)
                )
                .border(
                    2.dp,
                    Color(0xFF7C45E2),
                    RoundedCornerShape(5.dp)
                )
                .then(if (fieldHeight != null) Modifier.height(fieldHeight).padding(horizontal = 10.dp) else Modifier.padding(10.dp, 11.dp))
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.labelLarge,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) { innerTextField() }
            }
        )
    }
}
