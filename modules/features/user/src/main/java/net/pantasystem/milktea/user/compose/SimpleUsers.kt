package net.pantasystem.milktea.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.common_compose.AvatarIcon
import net.pantasystem.milktea.common_compose.CustomEmojiText
import net.pantasystem.milktea.model.user.User

@Composable
fun SimpleUserListView(
    modifier: Modifier = Modifier,
    users: List<User>,
    accountHost: String?,
    onSelected: (User) -> Unit,
    selectedUserIds: Set<User.Id> = emptySet(),
) {
    LazyColumn(modifier) {
        items(count = users.size) { index ->
            ItemSimpleUserCard(
                user = users[index],
                onSelected = onSelected,
                isSelected = selectedUserIds.contains(users[index].id),
                accountHost = accountHost,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSimpleUserCard(
    user: User,
    accountHost: String?,
    onSelected: (User) -> Unit,
    isSelected: Boolean = false,
) {

    Card(
        onClick = {
            onSelected.invoke(user)
        },
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.padding(0.5.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            AvatarIcon(url = user.avatarUrl, size = 50.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                CustomEmojiText(
                    text = user.displayName,
                    emojis = user.emojis,
                    accountHost = accountHost,
                    sourceHost = user.host,
                    parsedResult = user.parsedResult,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = user.displayUserName)
            }
        }
    }
}


@Preview
@Composable
fun PreviewItemSimpleUser() {
    ItemSimpleUserCard(user = User.Simple(
        avatarUrl = "https://pbs.twimg.com/profile_images/1377726964404908032/nHtGMU-X_400x400.jpg",
        userName = "harunon",
        name = null,
        emojis = emptyList(),
        host = "misskey.io",
        id = User.Id(0L, ""),
        isBot = true,
        isCat = true,
        nickname = null,
        isSameHost = true,
        instance = null,
        avatarBlurhash = null,
        badgeRoles = emptyList(),
    ), onSelected = {}, accountHost = "misskey.io"
    )
}