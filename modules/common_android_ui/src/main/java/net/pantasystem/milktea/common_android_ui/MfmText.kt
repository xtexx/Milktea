package net.pantasystem.milktea.common_android_ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import dev.misskey.mfm.node.MfmText as MfmTextNode
import dev.misskey.mfm.node.Plain
import dev.misskey.mfm.node.Quote
import dev.misskey.mfm.node.Search
import dev.misskey.mfm.node.Small
import dev.misskey.mfm.node.Strike
import dev.misskey.mfm.node.UnicodeEmoji
import dev.misskey.mfm.node.Url
import net.pantasystem.milktea.common_android.mfm.MFMParser
import net.pantasystem.milktea.model.emoji.CustomEmoji
import java.text.DateFormat
import java.util.Date

/**
 * MFM (Markup For Misskey) テキストを Compose の Text でリッチ表示するコンポーザブル。
 * 事前にパース済みの MfmNode リストを受け取るオーバーロード。
 *
 * タイムライン等でパース結果を ViewModel でキャッシュして再利用する場合はこちらを使用する。
 *
 * - テキスト装飾（太字・斜体・打消し線・小文字・中央揃え）は AnnotatedString の SpanStyle/ParagraphStyle で表現
 * - カスタム絵文字（:emoji_name:）は InlineTextContent + Coil で画像表示
 * - ruby/flip/rotate/border は Compose では再現困難なため子テキストのプレーン表示にフォールバック
 */
@Composable
fun MfmText(
    nodes: List<MfmNode>,
    modifier: Modifier = Modifier,
    emojiNameMap: Map<String, CustomEmoji> = emptyMap(),
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val baseFontSize = if (style.fontSize == TextUnit.Unspecified) 14.sp else style.fontSize

    // AnnotatedString 構築（non-composable なので remember 内で実行可能）
    val annotatedString = remember(nodes, emojiNameMap, primaryColor, onSurfaceVariantColor, baseFontSize) {
        buildAnnotatedString {
            appendMfmNodes(
                nodes = nodes,
                emojiNameMap = emojiNameMap,
                baseFontSize = baseFontSize.value,
                primaryColor = primaryColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
            )
        }
    }

    // EmojiCode ノードの ID → CustomEmoji マップを収集（InlineTextContent 構築に使用）
    val emojiEntries = remember(nodes, emojiNameMap) {
        buildEmojiMap(nodes, emojiNameMap)
    }

    // InlineTextContent は @Composable コンテンツを含むため remember 外で構築する
    val inlineContents = emojiEntries.entries.associate { (id, emoji) ->
        val url = emoji.url ?: emoji.uri
        id to InlineTextContent(
            placeholder = Placeholder(
                width = baseFontSize,
                height = baseFontSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
            )
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContents,
    )
}

/**
 * MFM (Markup For Misskey) テキストを Compose の Text でリッチ表示するコンポーザブル。
 * テキスト文字列を受け取り、内部で MFMParser によるパースを行うオーバーロード。
 *
 * パース結果を外部でキャッシュしない用途（ReplyPreview 等の簡易表示）に使用する。
 * パースに失敗した場合はプレーンテキストにフォールバックする。
 */
@Composable
fun MfmText(
    text: String,
    modifier: Modifier = Modifier,
    emojiNameMap: Map<String, CustomEmoji> = emptyMap(),
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val nodes = remember(text) { MFMParser.parse(text) }

    if (nodes == null) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
        )
        return
    }

    MfmText(
        nodes = nodes,
        modifier = modifier,
        emojiNameMap = emojiNameMap,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * ノードツリーを走査してカスタム絵文字の ID（":name:"）→ CustomEmoji マップを収集する。
 */
private fun buildEmojiMap(
    nodes: List<MfmNode>,
    emojiNameMap: Map<String, CustomEmoji>,
): Map<String, CustomEmoji> {
    val result = mutableMapOf<String, CustomEmoji>()
    fun traverse(nodes: List<MfmNode>) {
        for (node in nodes) {
            when (node) {
                is EmojiCode -> {
                    val emoji = emojiNameMap[node.name]
                    if (emoji != null) {
                        result[":${node.name}:"] = emoji
                    }
                }
                is Bold -> traverse(node.children)
                is Italic -> traverse(node.children)
                is Strike -> traverse(node.children)
                is Small -> traverse(node.children)
                is Center -> traverse(node.children)
                is Quote -> traverse(node.children)
                is Link -> traverse(node.children)
                is Fn -> traverse(node.children)
                else -> {}
            }
        }
    }
    traverse(nodes)
    return result
}

/**
 * MfmNode リストを再帰的に処理して AnnotatedString.Builder に追記する。
 * baseFontSize は sp 値（Float）として渡す。
 */
private fun AnnotatedString.Builder.appendMfmNodes(
    nodes: List<MfmNode>,
    emojiNameMap: Map<String, CustomEmoji>,
    baseFontSize: Float,
    primaryColor: Color,
    onSurfaceVariantColor: Color,
) {
    for (node in nodes) {
        appendMfmNode(node, emojiNameMap, baseFontSize, primaryColor, onSurfaceVariantColor)
    }
}

@Suppress("CyclomaticComplexMethod")
private fun AnnotatedString.Builder.appendMfmNode(
    node: MfmNode,
    emojiNameMap: Map<String, CustomEmoji>,
    baseFontSize: Float,
    primaryColor: Color,
    onSurfaceVariantColor: Color,
) {
    fun recurse(children: List<MfmNode>) {
        appendMfmNodes(children, emojiNameMap, baseFontSize, primaryColor, onSurfaceVariantColor)
    }

    when (node) {
        is MfmTextNode -> append(node.text)

        is Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            recurse(node.children)
        }

        is Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            recurse(node.children)
        }

        is Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            recurse(node.children)
        }

        is Small -> withStyle(SpanStyle(fontSize = (baseFontSize * 0.6f).sp)) {
            recurse(node.children)
        }

        is Center -> withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
            recurse(node.children)
        }

        is Quote -> withStyle(SpanStyle(color = onSurfaceVariantColor)) {
            append("│ ")
            recurse(node.children)
        }

        is Plain -> node.children.forEach { append(it.text) }

        is InlineCode -> withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color(0xFF1E1E1E),
                color = Color.White,
            )
        ) {
            append(node.code)
        }

        is CodeBlock -> withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color(0xFF1E1E1E),
                color = Color.White,
            )
        ) {
            append(node.code)
        }

        is Mention -> withStyle(SpanStyle(color = primaryColor)) {
            val host = node.host
            if (host == null) append("@${node.username}") else append("@${node.username}@$host")
        }

        is Hashtag -> withStyle(SpanStyle(color = primaryColor)) {
            append("#${node.hashtag}")
        }

        is Url -> withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)) {
            append(node.url)
        }

        is Link -> withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)) {
            recurse(node.children)
        }

        is UnicodeEmoji -> append(node.emoji)

        is EmojiCode -> {
            val id = ":${node.name}:"
            if (emojiNameMap.containsKey(node.name)) {
                appendInlineContent(id = id, alternateText = id)
            } else {
                append(id)
            }
        }

        is MathBlock -> append(node.formula)
        is MathInline -> append(node.formula)

        is Search -> withStyle(SpanStyle(color = primaryColor)) {
            append("${node.query} Search")
        }

        is Fn -> appendFnNode(node, emojiNameMap, baseFontSize, primaryColor, onSurfaceVariantColor)

        else -> {} // 未知ノードは無視
    }
}

private fun AnnotatedString.Builder.appendFnNode(
    node: Fn,
    emojiNameMap: Map<String, CustomEmoji>,
    baseFontSize: Float,
    primaryColor: Color,
    onSurfaceVariantColor: Color,
) {
    fun recurse(children: List<MfmNode>) {
        appendMfmNodes(children, emojiNameMap, baseFontSize, primaryColor, onSurfaceVariantColor)
    }

    when (node.name) {
        // サイズ拡大: プレビュー表示の崩れ防止のため 1.5x にキャップ
        "x2", "x3", "x4" -> withStyle(SpanStyle(fontSize = (baseFontSize * 1.5f).sp)) {
            recurse(node.children)
        }

        "fg" -> {
            val color = parseMfmColor(node.args["color"])?.let { Color(it) }
            if (color != null) {
                withStyle(SpanStyle(color = color)) { recurse(node.children) }
            } else {
                recurse(node.children)
            }
        }

        "bg" -> {
            val color = parseMfmColor(node.args["color"])?.let { Color(it) }
            if (color != null) {
                withStyle(SpanStyle(background = color)) { recurse(node.children) }
            } else {
                recurse(node.children)
            }
        }

        "font" -> {
            val family = when {
                node.args.containsKey("serif") -> FontFamily.Serif
                node.args.containsKey("monospace") -> FontFamily.Monospace
                node.args.containsKey("cursive") -> FontFamily.Cursive
                else -> null
            }
            if (family != null) {
                withStyle(SpanStyle(fontFamily = family)) { recurse(node.children) }
            } else {
                recurse(node.children)
            }
        }

        "unixtime" -> {
            val innerText = buildAnnotatedString { recurse(node.children) }.text
            val epochSec = innerText.trim().toLongOrNull()
            if (epochSec != null) {
                append(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(epochSec * 1000L))
                )
            } else {
                recurse(node.children)
            }
        }

        "scale" -> {
            val scaleY = node.args["y"]?.toFloatOrNull() ?: 1f
            val cappedScale = scaleY.coerceIn(0.5f, 1.5f)
            withStyle(SpanStyle(fontSize = (baseFontSize * cappedScale).sp)) {
                recurse(node.children)
            }
        }

        // ruby/flip/rotate/border/blur 等: 子テキストをプレーン表示
        else -> recurse(node.children)
    }
}

/**
 * MFM カラー文字列（3桁または6桁の HEX）を Android Color Int に変換する。
 * MFMDecorator.NodeDecorator.parseMfmColor と同一ロジック。
 */
private fun parseMfmColor(hex: String?): Int? {
    hex ?: return null
    return try {
        val expanded = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }
        AndroidColor.parseColor("#$expanded")
    } catch (_: IllegalArgumentException) {
        null
    }
}
