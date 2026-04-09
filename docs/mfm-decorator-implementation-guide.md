# MFM デコレーター 未実装構文 実装ガイド

対象ファイル: `modules/common_android_ui/src/main/java/net/pantasystem/milktea/common_android_ui/MFMDecorator.kt`

MFM パーサーライブラリ: `com.github.pantasystem:mfm-kt:main-SNAPSHOT`
パース結果の型: `List<MfmNode>`（`dev.misskey.mfm.node` パッケージ）

`$[name.arg=val 内容]` 構文はすべて `Fn(name, args: Map<String, String?>, children)` ノードとして渡される。
実装は `NodeDecorator.decorateNode()` 内の `is Fn ->` ブランチの `when (node.name)` に追加する。

---

## 実装済み構文（参考）

| 構文 | Fn name | 実装済み処理 |
|---|---|---|
| `$[x2]` 〜 `$[x4]` | `"x2"` 〜 `"x4"` | `RelativeSizeSpan(2f〜4f)` |
| `$[fg.color=fff ...]` | `"fg"` | `ForegroundColorSpan` |
| `$[bg.color=fff ...]` | `"bg"` | `BackgroundColorSpan` |
| `$[font.serif ...]` 等 | `"font"` | `TypefaceSpan("serif")` 等 |

---

## 未実装構文の実装方針

### 1. `$[unixtime 1701356400]`

**難易度: 低**

`Fn` ではなく専用ノードになる可能性があるが、パーサーの実装によっては `Fn(name="unixtime", args={"1701356400": null})` か、あるいは `children` にテキストが入る形になる。実際には `children` の `MfmText.text` に数値文字列が入る。

```kotlin
"unixtime" -> {
    val epochSec = node.children
        .filterIsInstance<MfmText>()
        .firstOrNull()?.text?.trim()?.toLongOrNull()
    if (epochSec != null) {
        val formatted = java.text.DateFormat
            .getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
            .format(java.util.Date(epochSec * 1000L))
        SpannedString(formatted)
    } else {
        inner
    }
}
```

> **注意**: `unixtime` は `inner` を返さず、変換後のテキストを直接 `SpannedString` で返す。

---

### 2. `$[blur テキスト]`

**難易度: 低**

Android 標準の `MaskFilterSpan` + `BlurMaskFilter` で実現できる。
`BlurMaskFilter` はぼかし半径（px）と種類（`NORMAL` / `OUTER` / `INNER` / `SOLID`）を指定する。

```kotlin
"blur" -> {
    inner.setSpan(
        android.text.style.MaskFilterSpan(
            android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        ),
        0, inner.length, 0
    )
}
```

> **注意**: `MaskFilterSpan` はハードウェアアクセラレーション有効時に無視される場合がある。
> `textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)` を TextView 側に設定する必要がある。
> `DecorateTextHelper.kt` 内の `decorate()` BindingAdapter で対応するのが適切。

---

### 3. `$[scale.x=2 テキスト]` / `$[scale.y=2 テキスト]`

**難易度: 低〜中**

- **横スケール (`scale.x`)**: Android 標準の `ScaleXSpan(factor)` で実現可能。
- **縦スケール (`scale.y`)**: 標準 API にないため `RelativeSizeSpan` で代替するか、カスタム Span が必要。

```kotlin
"scale" -> {
    val scaleX = node.args["x"]?.toFloatOrNull()
    val scaleY = node.args["y"]?.toFloatOrNull()
    if (scaleX != null) {
        inner.setSpan(android.text.style.ScaleXSpan(scaleX), 0, inner.length, 0)
    }
    if (scaleY != null) {
        // 縦スケールは RelativeSizeSpan で近似（正確ではない）
        inner.setSpan(RelativeSizeSpan(scaleY), 0, inner.length, 0)
    }
}
```

---

### 4. `$[border.style=solid,width=4 テキスト]`

**難易度: 中**

`ReplacementSpan` を継承したカスタム Span を作成し、`draw()` で `Canvas.drawRect()` を使って枠線を描画する。

```kotlin
class MfmBorderSpan(
    private val style: String,   // solid / dotted / dashed / double / groove / ridge / inset / outset
    private val width: Int,      // px
    private val color: Int,      // ARGB
    private val radius: Float,   // 角丸 px
) : ReplacementSpan() {

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?) =
        paint.measureText(text, start, end).toInt() + width * 2

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val borderPaint = Paint(paint).apply {
            this.color = this@MfmBorderSpan.color
            this.style = Paint.Style.STROKE
            strokeWidth = width.toFloat()
            // dotted/dashed は PathEffect で対応
            if (this@MfmBorderSpan.style == "dotted") {
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            } else if (this@MfmBorderSpan.style == "dashed") {
                pathEffect = DashPathEffect(floatArrayOf(10f, 4f), 0f)
            }
        }
        val rect = RectF(x, top.toFloat(), x + getSize(paint, text, start, end, null), bottom.toFloat())
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        canvas.drawText(text, start, end, x + width, y.toFloat(), paint)
    }
}
```

`Fn` 分岐での呼び出し:
```kotlin
"border" -> {
    val style = node.args["style"] ?: "solid"
    val width = node.args["width"]?.toIntOrNull() ?: 1
    val color = parseMfmColor(node.args["color"]) ?: Color.BLACK
    val radius = node.args["radius"]?.toFloatOrNull() ?: 0f
    inner.setSpan(MfmBorderSpan(style, width, color, radius), 0, inner.length, 0)
}
```

---

### 5. `$[ruby 漢字 かんじ]`

**難易度: 中**

`ReplacementSpan` を継承して、本文の上部に小さいテキスト（ルビ）を描画する。
`Fn(name="ruby", args={}, children=[MfmText("漢字 かんじ")])` の形でパースされる。
スペースで区切って `children[0]` = 本文、`children[1]` = ルビとして扱う。

```kotlin
class MfmRubySpan(
    private val baseText: String,
    private val rubyText: String,
) : ReplacementSpan() {

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return maxOf(
            paint.measureText(baseText),
            paint.measureText(rubyText) * (paint.textSize * 0.5f / paint.textSize)
        ).toInt()
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        // ルビ（上部小テキスト）
        val rubyPaint = Paint(paint).apply { textSize *= 0.5f }
        canvas.drawText(rubyText, x, top + rubyPaint.textSize, rubyPaint)
        // 本文
        canvas.drawText(baseText, x, y.toFloat(), paint)
    }
}
```

`Fn` 分岐での呼び出し:
```kotlin
"ruby" -> {
    val raw = node.children.filterIsInstance<MfmText>().joinToString("") { it.text }
    val spaceIdx = raw.lastIndexOf(' ')
    if (spaceIdx > 0) {
        val base = raw.substring(0, spaceIdx)
        val ruby = raw.substring(spaceIdx + 1)
        val spanned = SpannableString(base)
        spanned.setSpan(MfmRubySpan(base, ruby), 0, base.length, 0)
        spanned
    } else {
        inner
    }
}
```

---

### 6. `$[flip テキスト]` / `$[flip.v ...]` / `$[flip.h,v ...]`

**難易度: 中**

`ReplacementSpan` で `Canvas.scale(-1f, 1f)` (水平) または `Canvas.scale(1f, -1f)` (垂直) を使用。

```kotlin
class MfmFlipSpan(
    private val horizontal: Boolean,
    private val vertical: Boolean,
) : ReplacementSpan() {

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?) =
        paint.measureText(text, start, end).toInt()

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val w = paint.measureText(text, start, end)
        val cx = x + w / 2f
        val cy = (top + bottom) / 2f
        canvas.save()
        canvas.scale(
            if (horizontal) -1f else 1f,
            if (vertical) -1f else 1f,
            cx, cy
        )
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
        canvas.restore()
    }
}
```

`Fn` 分岐での呼び出し:
```kotlin
"flip" -> {
    val h = !node.args.containsKey("v") || node.args.containsKey("h")
    val v = node.args.containsKey("v")
    inner.setSpan(MfmFlipSpan(h, v), 0, inner.length, 0)
}
```

---

### 7. `$[rotate.deg=30 テキスト]`

**難易度: 中〜高**

`ReplacementSpan` で `Canvas.rotate(deg)` を使用。
回転するとバウンディングボックスが変わるため、`getSize()` での幅計算が複雑になる。

```kotlin
class MfmRotateSpan(private val degrees: Float) : ReplacementSpan() {

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?) =
        paint.measureText(text, start, end).toInt()

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val w = paint.measureText(text, start, end)
        val cx = x + w / 2f
        val cy = (top + bottom) / 2f
        canvas.save()
        canvas.rotate(degrees, cx, cy)
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
        canvas.restore()
    }
}
```

`Fn` 分岐での呼び出し:
```kotlin
"rotate" -> {
    val deg = node.args["deg"]?.toFloatOrNull() ?: 0f
    inner.setSpan(MfmRotateSpan(deg), 0, inner.length, 0)
}
```

---

## 実装しない構文（理由）

| 構文 | 理由 |
|---|---|
| `$[jelly]` `$[tada]` `$[jump]` `$[bounce]` `$[spin]` `$[shake]` `$[twitch]` `$[rainbow]` `$[sparkle]` | TextView の Span はアニメーション非対応。Compose や ObjectAnimator を使った別アーキテクチャが必要 |
| `$[position.x=0.8,y=0.5 ...]` | フローレイアウト内の絶対座標オフセットは TextView の行レイアウト制約上ほぼ不可能 |

---

## カスタム Span を配置するファイルの推奨場所

新規ファイルとして以下に作成する:

```
modules/common_android_ui/src/main/java/net/pantasystem/milktea/common_android_ui/mfm/
  MfmBorderSpan.kt
  MfmRubySpan.kt
  MfmFlipSpan.kt
  MfmRotateSpan.kt
```

`blur` 対応時の `LAYER_TYPE_SOFTWARE` 設定場所:
`DecorateTextHelper.kt` の `TextView.decorate(result: LazyDecorateResult?)` 内で
`LazyDecorateResult` にフラグを持たせるか、`Spanned` を検査して `MaskFilterSpan` があれば設定する。
