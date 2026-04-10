# NoteEditorReplyPreview MFM リッチ表示 実装計画書

## 概要

`NoteEditorReplyPreview` のノート本文表示を、現在のプレーンテキストから MFM 構文対応のリッチ表示に変更する。
Compose の `Text` コンポーザブル + `AnnotatedString` を主軸とし、`MFMDecorator`（TextView/Span ベース）を参考に Compose ネイティブな実装を行う。

---

## 実装方針

### アーキテクチャ

```
MFMParser.parse(text)          // common_android: 既存
    ↓ List<MfmNode>
buildMfmAnnotatedString()      // 【新規】common_android_ui: AnnotatedString を構築
    ↓ AnnotatedString + Map<id, InlineTextContent>
MfmText コンポーザブル          // 【新規】common_android_ui: Text + InlineTextContent でレンダリング
    ↓
NoteEditorReplyPreview.kt 更新 // 既存ファイル: MfmText に置き換え
```

### 配置モジュール

新規ファイルは `modules/common_android_ui` に追加する。
理由：
- `MFMParser`（`common_android`）と mfm-kt ノード型への依存が必要
- `common_android_ui` はすでに両者に依存しており、循環依存が発生しない
- `note` モジュールはすでに `common_android_ui` に依存済み

---

## 新規ファイル

### `MfmText.kt`（`common_android_ui` モジュール）

```
modules/common_android_ui/src/main/java/net/pantasystem/milktea/common_android_ui/MfmText.kt
```

#### エントリーポイント

```kotlin
@Composable
fun MfmText(
    text: String,
    modifier: Modifier = Modifier,
    emojiNameMap: Map<String, CustomEmoji> = emptyMap(),
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
)
```

#### 内部関数

```kotlin
// MfmNode リストを AnnotatedString に変換するビルダー
// InlineTextContent（カスタム絵文字用）の ID→コンテンツ も同時に収集する
private fun AnnotatedString.Builder.appendMfmNodes(
    nodes: List<MfmNode>,
    baseStyle: SpanStyle,
    inlineContents: MutableMap<String, InlineTextContent>,
    emojiNameMap: Map<String, CustomEmoji>,
    fontSize: TextUnit,
)
```

---

## MFM ノード対応表

### Compose `AnnotatedString` で対応可能なノード

| MfmNode | 対応方法 |
|---|---|
| `MfmText` | `append(text)` |
| `Bold` | `SpanStyle(fontWeight = FontWeight.Bold)` |
| `Italic` | `SpanStyle(fontStyle = FontStyle.Italic)` |
| `Strike` | `SpanStyle(textDecoration = TextDecoration.LineThrough)` |
| `Small` | `SpanStyle(fontSize = base * 0.6f)` |
| `Center` | `ParagraphStyle(textAlign = TextAlign.Center)` |
| `InlineCode` | `SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF1E1E1E), color = Color.White)` |
| `CodeBlock` | InlineCode と同様（プレビュー用簡略表示） |
| `Mention` | `SpanStyle(color = primary)` + テキスト `@user` |
| `Hashtag` | `SpanStyle(color = primary)` + テキスト `#tag` |
| `Url` | `SpanStyle(color = primary, textDecoration = Underline)` |
| `Link` | `SpanStyle(color = primary, textDecoration = Underline)` |
| `UnicodeEmoji` | `append(emoji)` (Compose ネイティブで表示) |
| `EmojiCode` | `appendInlineContent(id)` + `InlineTextContent` に Coil 画像 |
| `Plain` | `append(children.text)` |
| `MathBlock` | `append(formula)` プレーンテキスト |
| `MathInline` | `append(formula)` プレーンテキスト |
| `Search` | `append("${query} Search")` |
| `Quote` | `SpanStyle(color = onSurfaceVariant)` + 先頭に `"│ "` プレフィックス |
| `Fn("x2")` | `SpanStyle(fontSize = base * 2f)` |
| `Fn("x3")` | `SpanStyle(fontSize = base * 3f)` |
| `Fn("x4")` | `SpanStyle(fontSize = base * 4f)` |
| `Fn("fg")` | `SpanStyle(color = parsedColor)` |
| `Fn("bg")` | `SpanStyle(background = parsedColor)` |
| `Fn("font")` | `SpanStyle(fontFamily = serif/monospace/cursive/fantasy)` |
| `Fn("unixtime")` | epoch 秒を日時文字列に変換して `append()` |
| `Fn("scale")` | `SpanStyle(fontSize = base * scaleY)` + 子ノードへ適用 |

### Compose では再現困難なため省略（子ノードのプレーンテキストにフォールバック）

| MfmNode | 理由 |
|---|---|
| `Fn("ruby")` | `ReplacementSpan` 相当の Layout 操作が必要 |
| `Fn("flip")` | Canvas の scale(-1, 1) が必要 |
| `Fn("rotate")` | Canvas の rotate() が必要 |
| `Fn("border")` | カスタム描画が必要 |

> 将来的に `drawBehind` + カスタム Layout で対応可能だが、プレビュー用途には不要。

---

## `NoteEditorReplyPreview.kt` の変更点

### Before

```kotlin
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
```

### After

```kotlin
val text = note.text
if (!text.isNullOrEmpty()) {
    MfmText(
        text = text,
        emojiNameMap = replyTo.toShowNote.note.emojiNameMap ?: emptyMap(),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

---

## カスタム絵文字の処理方針

`CustomEmojiText.kt` と同じ方式：

1. `EmojiCode` ノードを検出した際、一意な ID（`:emoji_name:`）を `appendInlineContent(id, altText)` でプレースホルダーとして埋め込む
2. `emojiNameMap[name]` で `CustomEmoji` を解決し、URL を取得
3. `InlineTextContent` に Coil の `rememberAsyncImagePainter` を使って画像を配置
4. 解決できない場合は `:name:` テキストにフォールバック

---

## 注意事項

- `maxLines = 3` は Compose の `Text` がそのまま処理するため、長文でも行数制限は機能する
- `Fn("x2/x3/x4")` は本文内の一部テキストが極端に大きくなるため、プレビューでは `coerceAtMost(base * 1.5f)` のようなキャップを設ける（視覚的に崩れにくくする）
- `MFMParser.parse()` は IO でないが念のため `remember(text)` でキャッシュする
- `Color.parseColor()` は `android.graphics.Color` のため Compose の `Color` への変換が必要（`Color(androidColor.toULong())` ではなく `Color(androidColor)` で OK）

---

## 変更ファイル一覧

| ファイル | 変更種別 |
|---|---|
| `modules/common_android_ui/src/main/java/.../MfmText.kt` | **新規作成** |
| `modules/features/note/src/main/java/.../editor/NoteEditorReplyPreview.kt` | **変更** |

---

## 実装チェックリスト

- [ ] `MfmText.kt` 新規作成
  - [ ] `buildMfmAnnotatedString` 内部関数（再帰ノード処理）
  - [ ] テキスト装飾ノード対応（Bold / Italic / Strike / Small / Center）
  - [ ] コードノード対応（InlineCode / CodeBlock）
  - [ ] リンク系ノード対応（Mention / Hashtag / Url / Link）
  - [ ] 絵文字ノード対応（UnicodeEmoji / EmojiCode + InlineTextContent）
  - [ ] Fn ノード対応（x2/x3/x4 / fg / bg / font / unixtime / scale）
  - [ ] フォールバック処理（ruby / flip / rotate / border → 子テキスト表示）
  - [ ] `MfmText` コンポーザブル本体
- [ ] `NoteEditorReplyPreview.kt` 更新
  - [ ] `MfmText` に差し替え
  - [ ] `emojiNameMap` を `toShowNote.note.emojiNameMap` から取得
- [ ] ビルド確認（`./gradlew :modules:features:note:compileDebugKotlin`）
