package net.pantasystem.milktea.drive

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.pantasystem.milktea.common_compose.drive.EditCaptionDialogLayout
import net.pantasystem.milktea.common_compose.drive.EditFileNameDialogLayout
import net.pantasystem.milktea.model.drive.FileProperty

@Composable
@Stable
fun FileActionDropdownMenu(
    property: FileProperty,
    expanded: Boolean,
    onAction: (FileCardDropdownMenuAction) -> Unit
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            onAction(FileCardDropdownMenuAction.OnDismissRequest)
        },
        modifier = Modifier.wrapContentWidth(),
    ) {
        DropdownMenuItem(
            text = {
                if (property.isSensitive) Text(stringResource(R.string.drive_undo_nsfw))
                else Text(stringResource(R.string.drive_mark_as_nsfw))
            },
            leadingIcon = {
                if (property.isSensitive) Icon(Icons.Default.Image, contentDescription = stringResource(R.string.drive_undo_nsfw), modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.HideImage, contentDescription = stringResource(R.string.drive_mark_as_nsfw), modifier = Modifier.size(24.dp))
            },
            onClick = { onAction(FileCardDropdownMenuAction.OnNsfwMenuItemClicked) }
        )

        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete)) },
            leadingIcon = { Icon(Icons.Default.Delete, modifier = Modifier.size(24.dp), contentDescription = stringResource(R.string.delete)) },
            onClick = { onAction(FileCardDropdownMenuAction.OnDeleteMenuItemClicked) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.edit_caption)) },
            leadingIcon = { Icon(Icons.Default.Comment, modifier = Modifier.size(24.dp), contentDescription = stringResource(R.string.edit_caption)) },
            onClick = { onAction(FileCardDropdownMenuAction.OnEditFileCaption) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.edit_file_name)) },
            leadingIcon = { Icon(Icons.Default.Edit, modifier = Modifier.size(24.dp), contentDescription = stringResource(id = R.string.edit_file_name)) },
            onClick = { onAction(FileCardDropdownMenuAction.OnEditFileName) }
        )
    }


}

@Composable
fun ConfirmDeleteFilePropertyDialog(
    isShow: Boolean,
    filename: String,
    onDismissRequest: () -> Unit,
    onConfirmed: () -> Unit,
) {
    if (isShow) {
        AlertDialog(
            onDismissRequest = onDismissRequest,

            title = {
                Text(stringResource(R.string.drive_file_deletion_confirmation))
            },
            confirmButton = {
                TextButton(onClick = onConfirmed) {
                    Text(stringResource(R.string.delete))
                }
            },
            text = {
                Text(stringResource(R.string.drive_do_u_want_2_delete_s, filename))
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}


@Composable
fun EditCaptionDialog(
    fileProperty: FileProperty?,
    onDismiss: () -> Unit,
    onSave: (FileProperty.Id, newCaption: String) -> Unit
) {

    var captionText: String by remember(fileProperty) {
        mutableStateOf(fileProperty?.comment ?: "")
    }
    if (fileProperty != null) {
        Dialog(onDismissRequest = onDismiss) {
            EditCaptionDialogLayout(value = captionText, onCancelButtonClicked = onDismiss, onTextChanged = {
                captionText = it
            }, onSaveButtonClicked = {
                onSave(fileProperty.id, captionText)
            })
        }
    }

}

@Composable
fun EditFileNameDialog(
    fileProperty: FileProperty?,
    onDismiss: () -> Unit,
    onSave: (FileProperty.Id, newName: String) -> Unit
) {
    var nameText: String by remember(fileProperty) {
        mutableStateOf(fileProperty?.name ?: "")
    }
    if (fileProperty != null) {
        Dialog(onDismissRequest = onDismiss) {
            EditFileNameDialogLayout(
                value = nameText,
                onTextChanged = {
                    nameText = it
                },
                onSaveButtonClicked = {
                    onSave(fileProperty.id, nameText)
                },
                onCancelButtonClicked = {
                    onDismiss()
                }
            )
        }

    }}

sealed interface FileCardDropdownMenuAction {
    object OnDismissRequest : FileCardDropdownMenuAction
    object OnNsfwMenuItemClicked : FileCardDropdownMenuAction
    object OnDeleteMenuItemClicked : FileCardDropdownMenuAction
    object OnEditFileCaption : FileCardDropdownMenuAction
    object OnEditFileName : FileCardDropdownMenuAction
}