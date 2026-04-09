package net.pantasystem.milktea.note.reaction.choices

import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.pantasystem.milktea.common.glide.GlideApp
import net.pantasystem.milktea.common_android.resource.getString
import net.pantasystem.milktea.common_android.ui.VisibilityHelper.setMemoVisibility
import net.pantasystem.milktea.common_android.ui.haptic.HapticFeedbackController
import net.pantasystem.milktea.model.note.reaction.LegacyReaction
import net.pantasystem.milktea.note.EmojiListItemType
import net.pantasystem.milktea.note.EmojiType
import net.pantasystem.milktea.note.R
import net.pantasystem.milktea.note.databinding.ItemEmojiChoiceBinding
import net.pantasystem.milktea.note.databinding.ItemEmojiListItemHeaderBinding
import net.pantasystem.milktea.note.reaction.CustomEmojiImageViewSizeHelper.applySizeByAspectRatio
import net.pantasystem.milktea.note.reaction.ImageAspectRatioCache
import net.pantasystem.milktea.note.reaction.SaveImageAspectRequestListener
import java.util.concurrent.Executors
import kotlin.math.abs

class EmojiListItemsAdapter(
    private val isApplyImageAspectRatio: Boolean,
    private val onEmojiSelected: (EmojiType) -> Unit,
    private val onEmojiLongClicked: (EmojiType) -> Boolean,
    private val baseItemSizeDp: Int = 28,
) : RecyclerView.Adapter<EmojiListItemsAdapter.VH>() {

    companion object {
        // この閾値以上の差分は DiffUtil を使わず全置換する
        private const val FULL_REPLACE_THRESHOLD = 200
    }

    private var items: List<EmojiListItemType> = emptyList()

    // 最後に submitList されたリストへの参照（古い diff 結果を破棄するために使用）
    @Volatile
    private var latestSubmittedList: List<EmojiListItemType> = emptyList()

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun submitList(newList: List<EmojiListItemType>) {
        latestSubmittedList = newList

        val oldList = items
        val sizeDiff = abs(oldList.size - newList.size)

        // 大きな差分（検索モード切替など）は即座に全置換
        if (sizeDiff >= FULL_REPLACE_THRESHOLD) {
            items = newList
            notifyDataSetChanged()
            return
        }

        // 小規模差分: バックグラウンドで DiffUtil を実行
        // detectMoves=false で移動検出を省略し計算を高速化
        bgExecutor.execute {
            val capturedLatest = latestSubmittedList
            if (capturedLatest !== newList) return@execute  // より新しいリクエストが来ていたらスキップ

            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size
                override fun getNewListSize() = newList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = oldList[oldItemPosition]
                    val new = newList[newItemPosition]
                    if (old is EmojiListItemType.EmojiItem && new is EmojiListItemType.EmojiItem) {
                        return old.emoji.areItemsTheSame(new.emoji)
                    }
                    return old == new
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == newList[newItemPosition]
                }
            }, false)

            mainHandler.post {
                if (latestSubmittedList !== newList) return@post  // 結果適用前に新しいリクエストが来ていたらスキップ
                items = newList
                result.dispatchUpdatesTo(this@EmojiListItemsAdapter)
            }
        }
    }

    val currentList: List<EmojiListItemType> get() = items

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is EmojiListItemType.EmojiItem -> ItemType.Emoji.ordinal
            is EmojiListItemType.Header -> ItemType.Header.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return when (ItemType.values()[viewType]) {
            ItemType.Header -> {
                val binding = ItemEmojiListItemHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderVH(binding)
            }
            ItemType.Emoji -> {
                val binding = DataBindingUtil.inflate<ItemEmojiChoiceBinding>(
                    LayoutInflater.from(parent.context),
                    R.layout.item_emoji_choice,
                    parent,
                    false
                )
                EmojiVH(binding, isApplyImageAspectRatio, baseItemSizeDp)
            }
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (val item = items[position]) {
            is EmojiListItemType.EmojiItem -> {
                (holder as EmojiVH).onBind(
                    item.emoji,
                    onEmojiLongClicked = onEmojiLongClicked,
                    onEmojiSelected = onEmojiSelected
                )
            }
            is EmojiListItemType.Header -> {
                (holder as HeaderVH).binding.categoryName.text =
                    item.label.getString(holder.binding.root.context)
            }
        }
    }

    sealed class VH(view: View) : RecyclerView.ViewHolder(view)

    class EmojiVH(
        val binding: ItemEmojiChoiceBinding,
        private val isApplyImageAspectRatio: Boolean,
        private val baseItemSizeDp: Int,
    ) : VH(binding.root) {

        fun onBind(
            item: EmojiType, onEmojiSelected: (EmojiType) -> Unit,
            onEmojiLongClicked: (EmojiType) -> Boolean,
        ) {
            when (item) {
                is EmojiType.CustomEmoji -> {
                    if (isApplyImageAspectRatio) {
                        binding.reactionImagePreview.applySizeByAspectRatio<LinearLayout.LayoutParams>(
                            baseItemSizeDp,
                            item.emoji.aspectRatio ?: ImageAspectRatioCache.get(
                                item.emoji.url ?: item.emoji.uri
                            )
                        )
                    }
                    if (item.emoji.cachePath == null) {
                        GlideApp.with(binding.reactionImagePreview.context)
                            .load(item.emoji.url ?: item.emoji.uri)
                            .addListener(
                                SaveImageAspectRequestListener(
                                    item.emoji,
                                    binding.root.context
                                )
                            )
                            .into(binding.reactionImagePreview)
                    } else {
                        GlideApp.with(binding.reactionImagePreview.context)
                            .load(item.emoji.cachePath)
                            .addListener(
                                SaveImageAspectRequestListener(
                                    item.emoji,
                                    binding.root.context
                                )
                            )
                            .error(
                                GlideApp.with(binding.reactionImagePreview.context)
                                    .load(item.emoji.url ?: item.emoji.uri)
                                    .addListener(
                                        SaveImageAspectRequestListener(
                                            item.emoji,
                                            binding.root.context
                                        )
                                    )
                            )
                            .into(binding.reactionImagePreview)
                    }

                    binding.reactionStringPreview.setMemoVisibility(View.GONE)
                    binding.reactionImagePreview.setMemoVisibility(View.VISIBLE)
                }
                is EmojiType.Legacy -> {
                    binding.reactionStringPreview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, baseItemSizeDp * 0.8f)
                    binding.reactionImagePreview.setMemoVisibility(View.GONE)
                    binding.reactionStringPreview.setMemoVisibility(View.VISIBLE)
                    binding.reactionStringPreview.text =
                        requireNotNull(LegacyReaction.reactionMap[item.type])
                }
                is EmojiType.UtfEmoji -> {
                    binding.reactionStringPreview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, baseItemSizeDp * 0.8f)
                    binding.reactionStringPreview.setMemoVisibility(View.VISIBLE)
                    binding.reactionImagePreview.setMemoVisibility(View.GONE)
                    binding.reactionStringPreview.text = item.code
                }
            }
            binding.root.setOnClickListener {
                HapticFeedbackController.performClickHapticFeedback(it)
                onEmojiSelected(item)
            }
            binding.root.setOnLongClickListener {
                HapticFeedbackController.performLongClickHapticFeedback(it)
                onEmojiLongClicked(item)
            }
            binding.executePendingBindings()
        }
    }

    class HeaderVH(val binding: ItemEmojiListItemHeaderBinding) : VH(binding.root)

    enum class ItemType {
        Header, Emoji
    }
}
