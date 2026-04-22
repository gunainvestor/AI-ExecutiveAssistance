package com.execos.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

@Composable
fun ExecOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 12,
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        colors = colors,
        shape = MaterialTheme.shapes.large,
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction,
        ),
    )
}
