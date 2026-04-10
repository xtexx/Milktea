package net.pantasystem.milktea.note.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.common_resource.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteEditorScheduleSection(
    scheduleDate: Date,
    onPickDateClicked: () -> Unit,
    onPickTimeClicked: () -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.reservation_at),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClearClicked) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.remove),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.warning_reservation_msg),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row {
            OutlinedButton(
                onClick = onPickDateClicked,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Text(dateFormat.format(scheduleDate))
            }
            OutlinedButton(onClick = onPickTimeClicked) {
                Text(timeFormat.format(scheduleDate))
            }
        }
    }
}

@Preview
@Composable
private fun Preview_NoteEditorScheduleSection() {
    MaterialTheme {
        Surface {
            NoteEditorScheduleSection(
                scheduleDate = Date(),
                onPickDateClicked = {},
                onPickTimeClicked = {},
                onClearClicked = {},
            )
        }
    }
}
