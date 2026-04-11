package net.pantasystem.milktea.common_android_ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dagger.hilt.android.EntryPointAccessors
import net.pantasystem.milktea.model.emoji.CustomEmojiParsedResult
import net.pantasystem.milktea.model.emoji.EmojiResolvedType

/**
 * カスタム絵文字（:emoji_name:）のみをインライン画像として描画し、
 * それ以外の文字はプレーンテキストとして表示する Composable。
 *
 * MfmText とは異なり、MFM 構文全体をパースするのではなく
 * [CustomEmojiParsedResult] の絵文字位置情報のみを使用するため、
 * 意図しない書式変換が発生しない。
 * ユーザーの表示名（displayName）の描画に適している。
 *
 * @param parsedResult [CustomEmojiParser.parse] で得られたパース結果
 * @param accountHost 自アカウントのホスト名（未解決絵文字の URL 生成に使用）
 */
@Composable
fun EmojiText(
    parsedResult: CustomEmojiParsedResult,
    accountHost: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val baseFontSize = if (style.fontSize == TextUnit.Unspecified) 14.sp else style.fontSize
    val effectiveStyle = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style

    val annotatedString = remember(parsedResult, baseFontSize) {
        buildAnnotatedString {
            var cursor = 0
            for (emojiPos in parsedResult.emojis) {
                if (cursor < emojiPos.start) {
                    append(parsedResult.text.substring(cursor, emojiPos.start))
                }
                val id = ":${emojiPos.result.tag}:"
                appendInlineContent(id = id, alternateText = id)
                cursor = emojiPos.end
            }
            if (cursor < parsedResult.text.length) {
                append(parsedResult.text.substring(cursor))
            }
        }
    }

    // InlineTextContent は @Composable コンテンツを含むため remember 外で構築する
    val context = LocalContext.current
    val inlineContents = parsedResult.emojis.associate { emojiPos ->
        val id = ":${emojiPos.result.tag}:"
        val url = emojiPos.result.getUrl(accountHost)
        val resolvedEmoji = (emojiPos.result as? EmojiResolvedType.Resolved)?.emoji
        val aspectRatio = when (val r = emojiPos.result) {
            is EmojiResolvedType.Resolved -> (r.emoji.aspectRatio ?: 1f).coerceIn(0.1f, 3f)
            is EmojiResolvedType.UnResolved -> 1f
        }
        id to InlineTextContent(
            placeholder = Placeholder(
                width = (baseFontSize.value * aspectRatio).sp,
                height = baseFontSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
            )
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    if (resolvedEmoji != null) {
                        val drawable = state.result.drawable
                        val imageAspectRatio =
                            drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                        val ep = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            BindingProvider::class.java,
                        )
                        ep.customEmojiAspectRatioStore().save(resolvedEmoji, imageAspectRatio)
                        ep.emojiImageCacheStore().save(resolvedEmoji)
                    }
                },
            )
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = effectiveStyle,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContents,
    )
}
