package net.pantasystem.milktea.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import net.pantasystem.milktea.common.ResultState
import net.pantasystem.milktea.common.StateContent
import net.pantasystem.milktea.messaging.viewmodel.MessageHistoryViewModel
import net.pantasystem.milktea.model.messaging.messagingId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageHistoryScreen(
    historyViewModel: MessageHistoryViewModel,
    onAction: (Action) -> Unit,
) {

    val uiState by historyViewModel.uiState.collectAsState()
    val isRefreshing by historyViewModel.isRefreshing.collectAsState()

    LaunchedEffect(key1 = null) {
        historyViewModel.loadGroupAndUser()
    }

    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection()),
        isRefreshing = isRefreshing,
        onRefresh = { historyViewModel.loadGroupAndUser() }
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when(val content = uiState.histories.content) {
                is StateContent.Exist -> {
                    val list = content.rawContent
                    items(content.rawContent.size, key = { list[it].messagingId }) { i ->
                        MessageHistoryCard(
                            history = list[i],
                            isUserNameDefault = uiState.isUserNameDefault,
                            onAction = onAction,
                        )
                    }
                }
                is StateContent.NotExist -> {
                    item {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (val state = uiState.histories) {
                                is ResultState.Error -> {
                                    Text("Load Error")
                                    Text(state.throwable.toString())
                                }
                                is ResultState.Fixed -> {
                                    Text("No content")
                                }
                                is ResultState.Loading -> {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

        }


    }

}