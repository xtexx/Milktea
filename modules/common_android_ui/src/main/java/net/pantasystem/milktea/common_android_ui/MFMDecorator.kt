package net.pantasystem.milktea.common_android_ui

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.graphics.Color
import java.net.URLDecoder
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.ScaleXSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import java.text.DateFormat
import java.util.Date
import android.view.View
import android.widget.TextView
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.internal.managers.FragmentComponentManager
import dev.misskey.mfm.node.Bold
import dev.misskey.mfm.node.Center
import dev.misskey.mfm.node.CodeBlock
import dev.misskey.mfm.node.EmojiCode
import dev.misskey.mfm.node.Fn
import dev.misskey.mfm.node.Hashtag
import dev.misskey.mfm.node.InlineCode
import dev.misskey.mfm.node.Italic
import dev.misskey.mfm.node.Link
import dev.misskey.mfm.node.MathBlock
import dev.misskey.mfm.node.MathInline
import dev.misskey.mfm.node.Mention
import dev.misskey.mfm.node.MfmNode
import dev.misskey.mfm.node.MfmText
import dev.misskey.mfm.node.Plain
import dev.misskey.mfm.node.Quote
import dev.misskey.mfm.node.Search
import dev.misskey.mfm.node.Small
import dev.misskey.mfm.node.Strike
import dev.misskey.mfm.node.UnicodeEmoji
import dev.misskey.mfm.node.Url
import net.pantasystem.milktea.common.glide.GlideApp
import net.pantasystem.milktea.common_android.emoji.V13EmojiUrlResolver
import net.pantasystem.milktea.common_android.mfm.MFMParser
import net.pantasystem.milktea.common_android.nyaize.nyaize
import net.pantasystem.milktea.common_android.ui.Activities
import net.pantasystem.milktea.common_android.ui.putActivity
import net.pantasystem.milktea.common_android.ui.text.DrawableEmojiSpan
import net.pantasystem.milktea.common_android.ui.text.EmojiAdapter
import net.pantasystem.milktea.common_navigation.SearchNavType
import net.pantasystem.milktea.common_navigation.UserDetailNavigationArgs
import net.pantasystem.milktea.model.emoji.CustomEmoji
import net.pantasystem.milktea.model.instance.HostWithVersion
import java.lang.ref.WeakReference
import kotlin.math.max

object MFMDecorator {

    fun decorate(
        textView: TextView,
        lazyDecorateResult: LazyDecorateResult?,
        customEmojiScale: Float = 1f,
        skipEmojis: SkipEmojiHolder = SkipEmojiHolder(),
    ): Spanned? {
        lazyDecorateResult ?: return null
        val emojiAdapter = EmojiAdapter(textView)
        textView.setTag(R.id.TEXT_VIEW_MFM_TAG_ID, lazyDecorateResult.sourceText)

        return LazyEmojiDecorator(
            WeakReference(textView),
            lazyDecorateResult,
            skipEmojis,
            emojiAdapter,
            customEmojiScale,
        ).decorate()
    }

    fun decorate(
        sourceText: String,
        nodes: List<MfmNode>,
        emojiNameMap: Map<String, CustomEmoji>,
        instanceEmojiNameMap: Map<String, CustomEmoji>,
        userHost: String?,
        accountHost: String?,
        isRequireProcessNyaize: Boolean,
        holder: LazyDecorateSkipElementsHolder,
    ): LazyDecorateResult {
        val spanned = NodeDecorator(
            emojiNameMap = emojiNameMap,
            instanceEmojiNameMap = instanceEmojiNameMap,
            userHost = userHost,
            accountHost = accountHost,
            isRequireProcessNyaize = isRequireProcessNyaize,
            holder = holder,
        ).decorateNodes(nodes)
        return LazyDecorateResult(
            sourceText = sourceText,
            spanned = spanned,
            skippedEmojis = holder.skipped.toList(),
        )
    }

    class NodeDecorator(
        private val emojiNameMap: Map<String, CustomEmoji>,
        private val instanceEmojiNameMap: Map<String, CustomEmoji>,
        private val userHost: String?,
        private val accountHost: String?,
        private val isRequireProcessNyaize: Boolean,
        private val holder: LazyDecorateSkipElementsHolder,
    ) {
        fun decorateNodes(nodes: List<MfmNode>, offset: Int = 0): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            var currentOffset = offset
            for (node in nodes) {
                val spanned = decorateNode(node, currentOffset)
                builder.append(spanned)
                currentOffset += spanned.length
            }
            return builder
        }

        private fun decorateNode(node: MfmNode, offset: Int): Spanned {
            return when (node) {
                is MfmText -> {
                    val text = if (isRequireProcessNyaize) nyaize(node.text) else node.text
                    SpannedString(text)
                }
                is Bold -> {
                    val inner = decorateNodes(node.children, offset)
                    inner.setSpan(StyleSpan(Typeface.BOLD), 0, inner.length, 0)
                    inner
                }
                is Italic -> {
                    val inner = decorateNodes(node.children, offset)
                    inner.setSpan(StyleSpan(Typeface.ITALIC), 0, inner.length, 0)
                    inner
                }
                is Strike -> {
                    val inner = decorateNodes(node.children, offset)
                    inner.setSpan(StrikethroughSpan(), 0, inner.length, 0)
                    inner
                }
                is Small -> {
                    val inner = decorateNodes(node.children, offset)
                    inner.setSpan(RelativeSizeSpan(0.6f), 0, inner.length, 0)
                    inner
                }
                is Center -> {
                    val inner = decorateNodes(node.children, offset)
                    inner.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, inner.length, 0)
                    inner
                }
                is Quote -> {
                    val inner = decorateNodes(node.children, offset)
                    inner.setSpan(QuoteSpan(), 0, inner.length, 0)
                    inner
                }
                is Plain -> {
                    val inner = SpannableStringBuilder()
                    node.children.forEach { inner.append(it.text) }
                    inner
                }
                is CodeBlock -> {
                    val spanned = SpannableString(node.code)
                    spanned.setSpan(BackgroundColorSpan(Color.parseColor("#000000")), 0, node.code.length, 0)
                    spanned.setSpan(ForegroundColorSpan(Color.WHITE), 0, node.code.length, 0)
                    spanned
                }
                is InlineCode -> {
                    val spanned = SpannableString(node.code)
                    spanned.setSpan(BackgroundColorSpan(Color.parseColor("#000000")), 0, node.code.length, 0)
                    spanned.setSpan(ForegroundColorSpan(Color.WHITE), 0, node.code.length, 0)
                    spanned
                }
                is Search -> makeClickableSpan("${node.query} Search") {
                    Intent(Intent.ACTION_SEARCH).apply {
                        putExtra(SearchManager.QUERY, node.query)
                    }
                }
                is Mention -> decorateMention(node)
                is Hashtag -> decorateHashtag(node)
                is Link -> {
                    val inner = decorateNodes(node.children, offset)
                    val url = node.url
                    inner.setSpan(object : ClickableSpan() {
                        override fun onClick(view: View) {
                            view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }, 0, inner.length, 0)
                    inner
                }
                is Url -> {
                    val convertedUrl = MFMParser.convertAppNoteUriIfGiveNoteUrl(accountHost, node.url)
                        ?: MFMParser.convertAppChannelUriIfGiveChannelUrl(accountHost, node.url)
                        ?: node.url
                    val displayUrl = try {
                        URLDecoder.decode(node.url, "UTF-8")
                    } catch (_: Exception) {
                        node.url
                    }
                    makeClickableSpan(displayUrl) {
                        Intent(Intent.ACTION_VIEW, Uri.parse(convertedUrl))
                    }
                }
                is EmojiCode -> decorateEmojiCode(node, offset)
                is UnicodeEmoji -> SpannedString(node.emoji)
                is MathBlock -> SpannedString(node.formula)
                is MathInline -> SpannedString(node.formula)
                is Fn -> {
                    val inner = decorateNodes(node.children, offset)
                    when (node.name) {
                        "x2" -> inner.setSpan(RelativeSizeSpan(2.0f), 0, inner.length, 0)
                        "x3" -> inner.setSpan(RelativeSizeSpan(3.0f), 0, inner.length, 0)
                        "x4" -> inner.setSpan(RelativeSizeSpan(4.0f), 0, inner.length, 0)
                        "fg" -> {
                            val color = parseMfmColor(node.args["color"])
                            if (color != null) {
                                inner.setSpan(ForegroundColorSpan(color), 0, inner.length, 0)
                            }
                        }
                        "bg" -> {
                            val color = parseMfmColor(node.args["color"])
                            if (color != null) {
                                inner.setSpan(BackgroundColorSpan(color), 0, inner.length, 0)
                            }
                        }
                        "font" -> {
                            val family = when {
                                node.args.containsKey("serif") -> "serif"
                                node.args.containsKey("monospace") -> "monospace"
                                node.args.containsKey("cursive") -> "cursive"
                                node.args.containsKey("fantasy") -> "fantasy"
                                else -> null
                            }
                            if (family != null) {
                                inner.setSpan(TypefaceSpan(family), 0, inner.length, 0)
                            }
                        }
                        "unixtime" -> {
                            val epochSec = inner.toString().trim().toLongOrNull()
                            if (epochSec != null) {
                                val formatted = DateFormat
                                    .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                    .format(Date(epochSec * 1000L))
                                return SpannedString(formatted)
                            }
                        }
                        "scale" -> {
                            val scaleX = node.args["x"]?.toFloatOrNull()
                            val scaleY = node.args["y"]?.toFloatOrNull()
                            if (scaleX != null) {
                                inner.setSpan(ScaleXSpan(scaleX), 0, inner.length, 0)
                            }
                            if (scaleY != null) {
                                inner.setSpan(RelativeSizeSpan(scaleY), 0, inner.length, 0)
                            }
                        }
                        "ruby" -> {
                            val raw = inner.toString()
                            val splitIndex = raw.lastIndexOf(' ')
                            if (splitIndex > 0) {
                                val baseText = raw.substring(0, splitIndex)
                                val rubyText = raw.substring(splitIndex + 1)
                                val spanned = SpannableString(baseText)
                                spanned.setSpan(
                                    MfmRubySpan(baseText, rubyText),
                                    0, baseText.length,
                                    0
                                )
                                return spanned
                            }
                        }
                        "flip" -> {
                            val h = !node.args.containsKey("v") || node.args.containsKey("h")
                            val v = node.args.containsKey("v")
                            inner.setSpan(MfmFlipSpan(h, v), 0, inner.length, 0)
                        }
                        "rotate" -> {
                            val deg = node.args["deg"]?.toFloatOrNull() ?: 0f
                            inner.setSpan(MfmRotateSpan(deg), 0, inner.length, 0)
                        }
                        "border" -> {
                            val style = node.args["style"] ?: "solid"
                            val width = node.args["width"]?.toFloatOrNull() ?: 1f
                            val color = parseMfmColor(node.args["color"]) ?: Color.BLACK
                            val radius = node.args["radius"]?.toFloatOrNull() ?: 0f
                            inner.setSpan(MfmBorderSpan(style, width, color, radius), 0, inner.length, 0)
                        }
                    }
                    inner
                }
                else -> SpannedString("")
            }
        }

        private fun decorateMention(mention: Mention): Spanned {
            val displayText = buildMentionText(mention)
            return makeClickableSpan(displayText) { view ->
                val activity = FragmentComponentManager.findActivity(view.context) as Activity
                val intent = EntryPointAccessors.fromActivity(
                    activity,
                    NavigationEntryPointForBinding::class.java
                ).userDetailNavigation()
                    .newIntent(UserDetailNavigationArgs.UserName(userName = displayText))
                intent.putActivity(Activities.ACTIVITY_IN_APP)
                intent
            }
        }

        private fun buildMentionText(mention: Mention): String {
            val mentionHost = mention.host
            return if (mentionHost == null) {
                "@${mention.username}"
            } else if (accountHost != null && mentionHost == accountHost && userHost == accountHost) {
                "@${mention.username}"
            } else {
                "@${mention.username}@$mentionHost"
            }
        }

        private fun decorateHashtag(hashtag: Hashtag): Spanned {
            val tag = "#${hashtag.hashtag}"
            return makeClickableSpan(tag) { view ->
                val activity = FragmentComponentManager.findActivity(view.context) as Activity
                val navigation = EntryPointAccessors.fromActivity(
                    activity,
                    NavigationEntryPointForBinding::class.java
                )
                navigation.searchNavigation()
                    .newIntent(SearchNavType.ResultScreen(tag))
            }
        }

        private fun decorateEmojiCode(node: EmojiCode, offset: Int): Spanned {
            val emoji = resolveEmoji(node.name) ?: return SpannedString(":${node.name}:")
            val text = ":${node.name}:"
            val spanned = SpannableString(text)
            holder.add(SkippedEmoji(spanned, emoji, offset, offset + text.length))
            return spanned
        }

        private fun resolveEmoji(name: String): CustomEmoji? {
            val resolved = emojiNameMap[name] ?: instanceEmojiNameMap[name]
            if (resolved != null) return resolved
            if (userHost.isNullOrBlank() || accountHost == userHost) return null
            if (!HostWithVersion.isOverV13(accountHost)) return null
            val url = V13EmojiUrlResolver.resolve(accountHost, name, userHost)
            return CustomEmoji(name = name, url = url, uri = url, host = userHost)
        }

        private fun parseMfmColor(hex: String?): Int? {
            hex ?: return null
            return try {
                val expanded = when (hex.length) {
                    3 -> hex.map { "$it$it" }.joinToString("")
                    6 -> hex
                    else -> return null
                }
                Color.parseColor("#$expanded")
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        private fun makeClickableSpan(text: String, makeIntent: (View) -> Intent): SpannableString {
            val spanned = SpannableString(text)
            spanned.setSpan(object : ClickableSpan() {
                override fun onClick(view: View) {
                    view.context.startActivity(makeIntent(view))
                }
            }, 0, text.length, 0)
            return spanned
        }
    }

    class LazyEmojiDecorator(
        private val textView: WeakReference<TextView>,
        private val lazyDecorateResult: LazyDecorateResult,
        private val skipEmojis: SkipEmojiHolder,
        private val emojiAdapter: EmojiAdapter,
        private val customEmojiScale: Float,
    ) {
        private val spannableString = SpannableString(lazyDecorateResult.spanned)

        fun decorate(): Spanned {
            lazyDecorateResult.skippedEmojis.forEach { decorateEmoji(it) }
            return spannableString
        }

        private fun decorateEmoji(skippedEmoji: SkippedEmoji) {
            val emoji = skippedEmoji.customEmoji
            if (skipEmojis.contains(emoji)) return
            textView.get()?.let { textView ->
                val emojiSpan = DrawableEmojiSpan(emojiAdapter, emoji.url, emoji.aspectRatio)
                spannableString.setSpan(emojiSpan, skippedEmoji.start, skippedEmoji.end, 0)
                spannableString.setSpan(RelativeSizeSpan(customEmojiScale), skippedEmoji.start, skippedEmoji.end, 0)
                val height = max(textView.textSize * 0.75f, 10f)
                val width = when (val aspectRatio = emoji.aspectRatio) {
                    null -> height
                    else -> height * aspectRatio
                }
                val windowWidthSize = textView.resources.displayMetrics.widthPixels
                val finalEmojiWidth: Int
                val finalEmojiHeight: Int
                if ((width * customEmojiScale).toInt() > windowWidthSize && windowWidthSize > 0) {
                    val scale = windowWidthSize.toFloat() / (width * customEmojiScale)
                    finalEmojiWidth = windowWidthSize
                    finalEmojiHeight = (height * customEmojiScale * scale).toInt()
                } else {
                    finalEmojiWidth = (width * customEmojiScale).toInt()
                    finalEmojiHeight = (height * customEmojiScale).toInt()
                }
                GlideApp.with(textView)
                    .load(emoji.cachePath)
                    .error(
                        GlideApp.with(textView)
                            .load(emoji.url ?: emoji.uri)
                            .override(finalEmojiWidth, finalEmojiHeight)
                    )
                    .override(finalEmojiWidth, finalEmojiHeight)
                    .into(emojiSpan.target)
            }
        }
    }
}

data class LazyDecorateResult(
    val sourceText: String,
    val spanned: Spanned,
    val skippedEmojis: List<SkippedEmoji>,
)

data class SkippedEmoji(
    val spanned: Spanned,
    val customEmoji: CustomEmoji,
    val start: Int,
    val end: Int,
)

class LazyDecorateSkipElementsHolder {
    val skipped = mutableListOf<SkippedEmoji>()
    fun add(skipped: SkippedEmoji) {
        this.skipped.add(skipped)
    }
}

class SkipEmojiHolder {
    private val skipEmojis = mutableSetOf<CustomEmoji>()
    fun add(emoji: CustomEmoji): SkipEmojiHolder {
        skipEmojis.add(emoji)
        return this
    }
    fun contains(emoji: CustomEmoji): Boolean = skipEmojis.contains(emoji)
}
