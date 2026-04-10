package jp.panta.misskeyandroidclient.media

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.apng.decode.APNGDecoder
import com.github.penfeizhou.animation.apng.decode.APNGParser
import com.github.penfeizhou.animation.io.ByteBufferReader
import com.github.penfeizhou.animation.loader.ByteBufferLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer

/**
 * API 28 未満向けの APNG 対応 Coil Decoder。
 *
 * penfeizhou animation ライブラリ（既に Glide 経由で使用中）を Coil の Decoder インターフェースでラップし、
 * Compose の AsyncImage / InlineTextContent 内で APNG アニメーションを表示できるようにする。
 *
 * API 28 以上では ImageDecoderDecoder が GIF・APNG 両方をネイティブに処理するためスキップする。
 *
 * PNG コンテンツに対して Factory が create() を返し、decode() 内で APNG かどうかを判定する:
 * - APNG の場合: APNGDrawable（アニメーション）を返す
 * - 静止 PNG の場合: BitmapDrawable にフォールバックする
 */
class CoilApngDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = withContext(Dispatchers.IO) {
            source.source().readByteArray()
        }

        val byteBuffer = ByteBuffer.wrap(bytes)

        // IEND チャンク以降のデータが残っていると penfeizhou が例外を投げるため
        // IEND チャンクの終端位置に limit を設定してトリムする
        val iendPos = findIendChunkPosition(bytes)
        if (iendPos >= 0) {
            byteBuffer.limit(iendPos + IEND_CHUNK.size)
        }

        // APNG かどうかを判定（ByteBuffer を消費しないよう duplicate() を使用）
        val isApng = APNGParser.isAPNG(ByteBufferReader(byteBuffer.duplicate()))

        return if (isApng) {
            val loader = object : ByteBufferLoader() {
                override fun getByteBuffer(): ByteBuffer {
                    byteBuffer.position(0)
                    return byteBuffer
                }
            }
            DecodeResult(
                drawable = APNGDrawable(APNGDecoder(loader, null)),
                isSampled = false,
            )
        } else {
            // 静止 PNG として BitmapFactory でデコード
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } ?: throw IOException("Failed to decode PNG as bitmap")
            DecodeResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = false,
            )
        }
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            // API 28 以上は ImageDecoderDecoder に委ねる
            if (Build.VERSION.SDK_INT >= 28) return null
            // PNG 系 MIME タイプのみ対象（GIF は GifDecoder が処理する）
            val mime = result.mimeType
            if (mime != null &&
                !mime.startsWith("image/png", ignoreCase = true) &&
                !mime.equals("image/apng", ignoreCase = true)
            ) return null
            return CoilApngDecoder(result.source, options)
        }
    }

    companion object {
        /** PNG ファイル末尾を示す IEND チャンク（長さ0 + "IEND" + CRC） */
        private val IEND_CHUNK = byteArrayOf(
            0x00, 0x00, 0x00, 0x00,
            0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60.toByte(), 0x82.toByte(),
        )

        /**
         * バイト配列の末尾から IEND チャンクを探して先頭インデックスを返す。
         * 見つからない場合は -1 を返す。
         */
        private fun findIendChunkPosition(bytes: ByteArray): Int {
            if (bytes.size < IEND_CHUNK.size) return -1
            val startPos = bytes.size - IEND_CHUNK.size
            for (i in startPos downTo 0) {
                if (bytes[i] == IEND_CHUNK[0]) {
                    val slice = bytes.sliceArray(i until (i + IEND_CHUNK.size))
                    if (slice.contentEquals(IEND_CHUNK)) return i
                }
            }
            return -1
        }
    }
}
