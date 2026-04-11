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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
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

    val listState = rememberLazyListState()

    // 末尾付近でスクロールしたら追加ロード
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

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.loadNew() },
        modifier = modifier.fillMaxSize(),
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
}
