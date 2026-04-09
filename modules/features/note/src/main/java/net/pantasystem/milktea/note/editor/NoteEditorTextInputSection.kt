package net.pantasystem.milktea.note.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.note.R

/**
 * テキスト入力エリア（CW フィールド + 本文フィールド）
 *
 * NOTE: Phase 2 でこの内部の TextField を EmojiAutoCompleteTextField（AndroidView ラッパー）に置き換える。
 */
@Composable
fun NoteEditorTextInputSection(
    text: String,
    cw: String?,
    hasCw: Boolean,
    onTextChanged: (String) -> Unit,
    onCwChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (hasCw) {
            OutlinedTextField(
                value = cw ?: "",
                onValueChange = onCwChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(id = R.string.cw_hint)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            placeholder = { Text(stringResource(id = R.string.please_speak)) },
            minLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Preview
@Composable
private fun Preview_NoteEditorTextInputSection() {
    MaterialTheme {
        Surface {
            NoteEditorTextInputSection(
                text = "",
                cw = null,
                hasCw = false,
                onTextChanged = {},
                onCwChanged = {},
            )
        }
    }
}

@Preview(name = "With CW")
@Composable
private fun Preview_NoteEditorTextInputSection_WithCw() {
    MaterialTheme {
        Surface {
            NoteEditorTextInputSection(
                text = "Hello, World!",
                cw = "content warning",
                hasCw = true,
                onTextChanged = {},
                onCwChanged = {},
            )
        }
    }
}
