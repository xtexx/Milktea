package net.pantasystem.milktea.setting.compose.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.pantasystem.milktea.common_android_ui.account.AccountSwitchingDialogLayout
import net.pantasystem.milktea.common_android_ui.account.AccountTile
import net.pantasystem.milktea.common_android_ui.account.viewmodel.AccountInfo
import net.pantasystem.milktea.common_android_ui.account.viewmodel.AccountViewModelUiState
import net.pantasystem.milktea.common_navigation.UserDetailNavigationArgs
import net.pantasystem.milktea.setting.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingScreen(
    uiState: AccountViewModelUiState,
    onAccountClicked: (AccountInfo) -> Unit,
    onAddAccountButtonClicked: () -> Unit,
    onNavigateUp: () -> Unit,
    onShowUser: (UserDetailNavigationArgs) -> Unit,
    onSignOutButtonClicked: (AccountInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Text(stringResource(id = R.string.account))
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.currentAccountInfo != null) {
                    AccountTile(
                        account = uiState.currentAccountInfo!!,
                        onClick = {
                            showBottomSheet = true
                        },
                        onAvatarClick = {
                            onShowUser(
                                UserDetailNavigationArgs.UserName(it.user?.userName ?: it.account.userName)
                            )
                        }
                    )
                    TextButton(
                        onClick = {
                            onSignOutButtonClicked(uiState.currentAccountInfo!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.sign_out), color = Color.Red)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Button(
                    onClick = {
                        showBottomSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(stringResource(id = R.string.switch_account))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onAddAccountButtonClicked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(stringResource(id = R.string.add_account))
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            AccountSwitchingDialogLayout(
                uiState = uiState,
                onSettingButtonClicked = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                },
                onAvatarIconClicked = { accountInfo ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                    onShowUser(UserDetailNavigationArgs.UserName(accountInfo.user?.let {
                        "@${it.userName}@${it.host}"
                    } ?: "@${accountInfo.account.userName}@${accountInfo.account.getHost()}"))
                },
                onAccountClicked = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                    onAccountClicked(it)
                },
                onAddAccountButtonClicked = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                    onAddAccountButtonClicked()
                }
            )
        }
    }
}
