package net.pantasystem.milktea.note.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import net.pantasystem.milktea.note.R
import net.pantasystem.milktea.note.timeline.viewmodel.TimelineListItem
import net.pantasystem.milktea.note.timeline.viewmodel.TimelineViewModel
import net.pantasystem.milktea.note.view.NoteCardAction

/**
 * Compose 製タイムライン。
 * TimelineViewModel の StateFlow を直接収集し、NoteCard で各ノートを描画する。
 *
 * 既存の RecyclerView + TimelineListAdapter の代替として開発検証用に使用する。
 * DebugFeatureFlags.isComposeTimelineEnabled() が true のとき TimelineFragment から呼ばれる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeTimeline(
    viewModel: TimelineViewModel,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.timelineListState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val config by viewModel.configState.collectAsState()
    val isVisibleNewPostsButton by viewModel.isVisibleNewPostsButton.collectAsState(initial = false)
    val nestedScrollInterop = rememberNestedScrollInteropConnection()
    val listState = rememberLazyListState()

    // ── 末尾付近に達したら追加ロード（loadOld） ───────────────────────
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { reachedEnd ->
                if (reachedEnd) viewModel.loadOld()
            }
    }

    // ── スクロール停止時に onScrollStateChanged を通知 ─────────────────
    // 先頭付近 (<=3) では loadFuture + streaming 再開、それ以外は streaming 停止。
    // releaseUnusedPages と scroll 位置保存も ViewModel 側で実施される。
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                viewModel.onScrollStateChanged(listState.firstVisibleItemIndex)
            }
    }

    // ── スクロール量を onScrolled で通知 ──────────────────────────────
    // ・position / offset を ViewModel に保存（スクロール位置復元用）
    // ・下方向スクロール (dy > 16) で新着ボタンを非表示
    // ・isScrollInProgress が false のとき（データ更新によるリスト再構成など）は
    //   dy=0 を渡してボタンが誤って消えないようにする
    LaunchedEffect(listState) {
        var prevIndex = 0
        var prevOffset = 0
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress,
            )
        }.collect { (index, offset, isScrollInProgress) ->
                val dy = if (isScrollInProgress) {
                    when {
                        index > prevIndex -> 100   // アイテム境界を超えて下スクロール
                        index < prevIndex -> -100  // アイテム境界を超えて上スクロール
                        else -> offset - prevOffset
                    }
                } else {
                    0  // ユーザー操作なし（データ更新等）: 位置だけ保存、ボタンは消さない
                }
                prevIndex = index
                prevOffset = offset
                viewModel.onScrolled(
                    dy = dy,
                    firstVisibleItemPosition = index,
                    offset = offset,
                )
            }
    }

    // ── ストリーミングで先頭に 1 件挿入されたとき自動スクロール ──────────
    // RecyclerView 版 AdapterDataObserver の onItemRangeInserted と同等のロジック:
    //   positionStart==0 && itemCount==1 && firstVisiblePosition==0 && streaming
    LaunchedEffect(listState) {
        var prevFirstKey: Any? = null
        var prevNoteCount = 0
        snapshotFlow { items }
            .collect { currentItems ->
                val noteCount = currentItems.count { it is TimelineListItem.Note }
                val firstKey = (currentItems.firstOrNull() as? TimelineListItem.Note)?.note?.uuid

                val isStreamingInsert =
                    firstKey != null
                        && firstKey != prevFirstKey
                        && prevFirstKey != null
                        && noteCount == prevNoteCount + 1                     // 1 件だけ増えた
                        && listState.firstVisibleItemIndex == 0               // 先頭を表示中
                        && viewModel.timelineStore.latestReceiveNoteId() != null  // streaming 由来

                if (isStreamingInsert) {
                    listState.scrollToItem(0)
                }

                prevFirstKey = firstKey
                prevNoteCount = noteCount
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadNew() },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollInterop),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = items,
                    key = { item ->
                        when (item) {
                            is TimelineListItem.Note -> item.note.uuid
                            is TimelineListItem.Loading -> "loading"
                            is TimelineListItem.Error -> "error_${item.hashCode()}"
                            TimelineListItem.Empty -> "empty"
                        }
                    },
                ) { item ->
                    when (item) {
                        is TimelineListItem.Note -> {
                            NoteCard(
                                note = item.note,
                                onAction = onAction,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (config.isEnableNoteDivider) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }

                        TimelineListItem.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .wrapContentSize(Alignment.Center),
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is TimelineListItem.Error -> {
                            Text(
                                text = item.throwable.localizedMessage ?: "Error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp),
                            )
                        }

                        TimelineListItem.Empty -> {
                            Text(
                                text = "No notes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── 新着ノートボタン ───────────────────────────────────────────
        // loadFuture で 10 件以上の新着があるときに表示。
        // タップで loadInit(ignoreSavedScrollPosition=true) を呼び先頭に戻る。
        if (isVisibleNewPostsButton) {
            FilledTonalButton(
                onClick = { viewModel.loadInit(ignoreSavedScrollPosition = true) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            ) {
                Text(text = stringResource(R.string.jump_to_new_post))
            }
        }
    }
}
