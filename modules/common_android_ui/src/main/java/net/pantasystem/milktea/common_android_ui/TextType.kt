package net.pantasystem.milktea.common_android_ui

import net.pantasystem.milktea.common_android.html.MastodonHTML
import net.pantasystem.milktea.common_android.html.MastodonHTMLParser
import net.pantasystem.milktea.common_android.mfm.MFMParser
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.emoji.CustomEmoji
import net.pantasystem.milktea.model.note.Note
import net.pantasystem.milktea.model.note.NoteRelation

sealed interface TextType {
    data class Misskey(val lazyDecorateResult: LazyDecorateResult?) : TextType
    data class Mastodon(
        val html: MastodonHTML,
        val mentions: List<Note.Type.Mastodon.Mention>,
        val tags: List<Note.Type.Mastodon.Tag>
    ) : TextType
}

fun getTextType(
    account: Account,
    note: NoteRelation,
    instanceEmojis: Map<String, CustomEmoji>?,
    isRequirePerformNyaize: Boolean = false,
): TextType? {
    return when (account.instanceType) {
        Account.InstanceType.MISSKEY, Account.InstanceType.FIREFISH -> {
            val text = note.note.text ?: return null
            val nodes = MFMParser.parse(text)
            val lazyDecorateResult = nodes?.let {
                MFMDecorator.decorate(
                    sourceText = text,
                    nodes = it,
                    emojiNameMap = note.note.emojiNameMap ?: emptyMap(),
                    instanceEmojiNameMap = instanceEmojis ?: emptyMap(),
                    userHost = note.user.host,
                    accountHost = account.getHost(),
                    isRequireProcessNyaize = isRequirePerformNyaize,
                    holder = LazyDecorateSkipElementsHolder(),
                )
            }
            TextType.Misskey(lazyDecorateResult)
        }
        Account.InstanceType.MASTODON, Account.InstanceType.PLEROMA -> {
            note.note.text?.let {
                val option = note.note.type as? Note.Type.Mastodon
                TextType.Mastodon(
                    MastodonHTMLParser.parse(
                        it,
                        note.note.emojis ?: emptyList(),
                        userHost = note.user.host,
                        accountHost = account.getHost()
                    ),
                    tags = option?.tags ?: emptyList(),
                    mentions = option?.mentions ?: emptyList()
                )
            }
        }
    }
}
