package net.pantasystem.milktea.setting.compose.tab

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import net.pantasystem.milktea.model.account.page.Page
import net.pantasystem.milktea.setting.R
import net.pantasystem.milktea.setting.viewmodel.page.PageCandidate
import net.pantasystem.milktea.setting.viewmodel.page.PageCandidateGroup


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabItemsListScreen(
    dragDropState: DragAndDropState,
    pageTypes: List<PageCandidateGroup>,
    list: List<Page>,
    onSelectPage: (PageCandidate) -> Unit,
    onOptionButtonClicked: (Page) -> Unit,
    onNavigateUp: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.add_to_tab))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onNavigateUp()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(stringResource(R.string.add_tab))
                },
                icon = {
                    Icon(painter = painterResource(R.drawable.ic_add_to_tab_24px), contentDescription = null)
                },
                onClick = {
                    showBottomSheet = true
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        TabItemsList(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            list = list,
            onOptionButtonClicked = { page ->
                onOptionButtonClicked(page)
            },
            dragDropState = dragDropState
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            TabItemSelectionDialog(
                modifier = Modifier.fillMaxSize(),
                items = pageTypes,
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                    onSelectPage(it)
                }
            )
        }
    }
}
