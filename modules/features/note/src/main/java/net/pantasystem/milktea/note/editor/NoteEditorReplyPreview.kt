package net.pantasystem.milktea.note.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.common_compose.AvatarIcon
import net.pantasystem.milktea.note.viewmodel.PlaneNoteViewData

@Composable
fun NoteEditorReplyPreview(
    replyTo: PlaneNoteViewData,
    modifier: Modifier = Modifier,
) {
    val note by replyTo.currentNote.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AvatarIcon(
                url = replyTo.avatarUrl,
                onAvatarClick = {},
                size = 32.dp,
                borderStrokeWidth = 1.dp,
                borderStrokeColor = Color.Gray,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = replyTo.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val text = note.text
                if (!text.isNullOrEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

@Preview
@Composable
private fun Preview_NoteEditorReplyPreview() {
    MaterialTheme {
        Surface {
            Text(
                text = "Reply preview placeholder",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
