package net.pantasystem.milktea.common_compose

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pantasystem.milktea.common.glide.blurhash.BlurHashDecoder

/**
 * デコード済み Bitmap を保持する LruCache（プロセス全体で共有）。
 * 最大 2MB まで保持し、古いエントリを自動的に evict する。
 * キー: "${hash}_${width}x${height}"
 */
private val blurhashBitmapCache = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private fun cacheKey(hash: String, width: Int, height: Int) = "${hash}_${width}x${height}"

/**
 * Blurhash 文字列を非同期でデコードし、[Painter] として返す Composable。
 *
 * - デコード結果は [LruCache] にキャッシュされ、同じ hash は再デコードしない。
 * - デコードは [Dispatchers.IO] 上で実行するため UI スレッドをブロックしない。
 * - [blurhash] が null の場合は null を返す（プレースホルダーなし）。
 *
 * @param blurhash Blurhash 文字列
 * @param width    デコードするビットマップの幅（px）。デフォルト 32
 * @param height   デコードするビットマップの高さ（px）。デフォルト 32
 */
@Composable
fun rememberBlurhashPainter(
    blurhash: String?,
    width: Int = 32,
    height: Int = 32,
): Painter? {
    if (blurhash == null) return null

    // Composition 時にキャッシュを同期チェック
    val cachedBitmap = remember(blurhash, width, height) {
        blurhashBitmapCache.get(cacheKey(blurhash, width, height))
    }

    val painter by produceState<Painter?>(
        initialValue = cachedBitmap?.let { BitmapPainter(it.asImageBitmap()) },
        key1 = blurhash,
        key2 = width,
        key3 = height,
    ) {
        if (cachedBitmap != null) {
            value = BitmapPainter(cachedBitmap.asImageBitmap())
            return@produceState
        }
        withContext(Dispatchers.IO) {
            val bitmap = BlurHashDecoder.decode(blurhash, width, height) ?: return@withContext
            blurhashBitmapCache.put(cacheKey(blurhash, width, height), bitmap)
            value = BitmapPainter(bitmap.asImageBitmap())
        }
    }

    return painter
}
