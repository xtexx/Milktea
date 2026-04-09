package net.pantasystem.milktea.common_android_ui

import dev.misskey.mfm.node.Link
import dev.misskey.mfm.node.MfmNode
import dev.misskey.mfm.node.Url
import net.pantasystem.milktea.common_android.html.MastodonHTML
import net.pantasystem.milktea.common_android.html.MastodonHTMLParser
import net.pantasystem.milktea.common_android.mfm.MFMParser
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.emoji.CustomEmoji
import net.pantasystem.milktea.model.note.Note
import net.pantasystem.milktea.model.note.NoteRelation

sealed interface TextType {
    data class Misskey(
        val lazyDecorateResult: LazyDecorateResult?,
        val nodes: List<MfmNode>,
    ) : TextType {
        fun getUrls(): List<String> = extractUrls(nodes)

        private fun extractUrls(nodes: List<MfmNode>): List<String> {
            return nodes.flatMap { node ->
                when (node) {
                    is Url -> listOf(node.url)
                    is Link -> listOf(node.url) + extractUrls(node.children)
                    else -> {
                        val children = when (node) {
                            is dev.misskey.mfm.node.Bold -> node.children
                            is dev.misskey.mfm.node.Italic -> node.children
                            is dev.misskey.mfm.node.Strike -> node.children
                            is dev.misskey.mfm.node.Small -> node.children
                            is dev.misskey.mfm.node.Center -> node.children
                            is dev.misskey.mfm.node.Quote -> node.children
                            is dev.misskey.mfm.node.Fn -> node.children
                            else -> emptyList()
                        }
                        extractUrls(children)
                    }
                }
            }
        }
    }
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
            val nodes = MFMParser.parse(text) ?: emptyList()
            val lazyDecorateResult = nodes.takeIf { it.isNotEmpty() }?.let {
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
            TextType.Misskey(lazyDecorateResult, nodes)
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
