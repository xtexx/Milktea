package net.pantasystem.milktea.note.editor

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.common_viewmodel.UserViewData
import net.pantasystem.milktea.note.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorAddressSection(
    addressUsers: List<UserViewData>,
    onAddAddressClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier.padding(horizontal = 8.dp)) {
        for (userViewData in addressUsers) {
            val user by userViewData.user.collectAsState()
            val displayName = user?.displayName ?: userViewData.userName ?: ""
            AssistChip(
                onClick = {},
                label = { Text(displayName) },
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        TextButton(onClick = onAddAddressClicked) {
            Text(stringResource(id = R.string.add_address))
        }
    }
}

@Preview
@Composable
private fun Preview_NoteEditorAddressSection() {
    MaterialTheme {
        Surface {
            NoteEditorAddressSection(
                addressUsers = emptyList(),
                onAddAddressClicked = {},
            )
        }
    }
}
