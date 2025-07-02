package org.judging_app.ui.input

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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.judging_app.State

@Composable
fun TextInputComponent(
    labelText: String? = null
) {
    Column(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth(0.8f)
    ) {
        if (labelText != null) {
            Text(
                labelText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(5.dp))
        }
        val input = remember { mutableStateOf(State.judgeSurname) }
        BasicTextField(
            value = input.value,
            onValueChange = {
                    it: String ->
                        State.judgeSurname = it
                        input.value = it
            },
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
                .padding(10.dp, 11.dp)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}