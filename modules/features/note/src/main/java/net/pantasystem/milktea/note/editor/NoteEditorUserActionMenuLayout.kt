package net.pantasystem.milktea.note.editor

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.note.R

@Composable
fun NoteEditorUserActionMenuLayout(
    modifier: Modifier = Modifier,
    isEnableDrive: Boolean,
    iconColor: Color,
    isCw: Boolean,
    isPoll: Boolean,
    onPickFileFromDriveButtonClicked: () -> Unit,
    onPickFileFromLocalButtonCLicked: () -> Unit,
    onPickImageFromLocalButtonClicked: () -> Unit,
    onTogglePollButtonClicked: () -> Unit,
    onSelectMentionUsersButtonClicked: () -> Unit,
    onSelectEmojiButtonClicked: () -> Unit,
    onToggleCwButtonClicked: () -> Unit,
    onSelectDraftNoteButtonClicked: () -> Unit,
) {
    var isShowFilePickerDropDownMenu: Boolean by remember {
        mutableStateOf(false)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        MenuItemLayout {
            DropdownMenu(
                expanded = isShowFilePickerDropDownMenu,
                onDismissRequest = { isShowFilePickerDropDownMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.pick_image)) },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                    onClick = {
                        isShowFilePickerDropDownMenu = false
                        onPickImageFromLocalButtonClicked()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.pick_file_from_device)) },
                    leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    onClick = {
                        isShowFilePickerDropDownMenu = false
                        onPickFileFromLocalButtonCLicked()
                    }
                )
                if (isEnableDrive) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.pick_image_from_drive)) },
                        leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                        onClick = {
                            isShowFilePickerDropDownMenu = false
                            onPickFileFromDriveButtonClicked()
                        }
                    )
                }
            }
            IconButton(
                onClick = {
                    isShowFilePickerDropDownMenu = true
                }
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = iconColor
                )
            }
        }
        MenuItemLayout {
            IconButton(onClick = onTogglePollButtonClicked) {
                Icon(
                    Icons.Default.Poll,
                    contentDescription = null,
                    tint = if (isPoll) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        iconColor
                    }
                )
            }
        }
        MenuItemLayout {
            IconButton(onClick = onToggleCwButtonClicked) {
                Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = if (isCw) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        iconColor
                    }
                )
            }
        }
        MenuItemLayout {
            IconButton(onClick = onSelectMentionUsersButtonClicked) {
                Icon(
                    painterResource(id = R.drawable.ic_mention),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }
        }

        MenuItemLayout {
            IconButton(onClick = onSelectDraftNoteButtonClicked) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = iconColor
                )
            }
        }

        MenuItemLayout {
            IconButton(onClick = onSelectEmojiButtonClicked) {
                Icon(
                    Icons.Default.EmojiEmotions,
                    contentDescription = null,
                    tint = iconColor
                )
            }
        }

    }
}

@Composable
fun MenuItemLayout(
    modifier: Modifier = Modifier,
    children: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        children()
    }
}

@Preview
@Composable
fun Preview_NoteEditorUserActionMenuLayout() {
    MaterialTheme {
        NoteEditorUserActionMenuLayout(
            modifier = Modifier.height(56.dp),
            isEnableDrive = true,
            iconColor = Color.Gray,
            isCw = true,
            isPoll = true,
            onPickFileFromDriveButtonClicked = {},
            onPickFileFromLocalButtonCLicked = {},
            onPickImageFromLocalButtonClicked = {},
            onTogglePollButtonClicked = {},
            onSelectMentionUsersButtonClicked = {},
            onSelectEmojiButtonClicked = {},
            onToggleCwButtonClicked = {},
            onSelectDraftNoteButtonClicked = {}
        )
    }
}

@Composable
fun getColor(color: Int): Color {
    return colorResource(LocalContext.current.getColorFromAttrs(color).resourceId)
}

fun Context.getColorFromAttrs(attr: Int): TypedValue {
    return TypedValue().apply {
        theme.resolveAttribute(attr, this, true)
    }
}