package net.pantasystem.milktea.note.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.pantasystem.milktea.note.R

/**
 * コンテンツが [maxHeight] を超えた場合に折り畳み、展開ボタンを下部中央に表示する。
 * 展開ボタンがタップされると [onExpand] が呼ばれる。
 * 既存の AutoCollapsingLayout (View) の Compose 版。
 *
 * [expanded] / [onExpand] を PlaneNoteViewData.expanded / expand() に繋ぐことで、
 * スクロールによる再コンポーズ後も展開状態が保持される。
 *
 * 実装は SubcomposeLayout による2スロット構成:
 *   1. "content" — 高さ無制限で計測しオーバーフロー判定
 *   2. "overlay" — オーバーフロー時のみグラデーションスクリム + 展開ボタンを計測・配置
 */
@Composable
fun AutoCollapsingLayout(
    expanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 300.dp,
    content: @Composable () -> Unit,
) {
    val maxHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
    val overlayHeightDp = 56.dp
    val overlayHeightPx = with(LocalDensity.current) { overlayHeightDp.roundToPx() }
    val surfaceColor = MaterialTheme.colorScheme.surface

    SubcomposeLayout(
        modifier = modifier.clipToBounds(),
    ) { constraints ->
        // 1. コンテンツを高さ無制限で計測してオーバーフローを検出
        val contentPlaceables = subcompose("content", content).map {
            it.measure(constraints.copy(maxHeight = Int.MAX_VALUE))
        }
        val naturalHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0
        val overflows = !expanded && naturalHeight > maxHeightPx
        val layoutHeight = if (overflows) maxHeightPx else naturalHeight

        // 2. オーバーフロー時のみグラデーションスクリム + 展開ボタンを計測
        val overlayPlaceables = if (overflows) {
            subcompose("overlay") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(overlayHeightDp),
                ) {
                    // グラデーションスクリム（コンテンツが途中で途切れる感を和らげる）
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, surfaceColor),
                                )
                            )
                    )
                    // 展開ボタン
                    TextButton(
                        onClick = onExpand,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        Text(
                            text = stringResource(R.string.expand),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }.map {
                it.measure(
                    constraints.copy(minHeight = 0, maxHeight = overlayHeightPx)
                )
            }
        } else {
            emptyList()
        }

        layout(constraints.maxWidth, layoutHeight) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            // オーバーレイをレイアウト下端に貼り付ける
            overlayPlaceables.forEach {
                it.placeRelative(x = 0, y = layoutHeight - it.height)
            }
        }
    }
}
