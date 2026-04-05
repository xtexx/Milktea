package net.pantasystem.milktea.setting.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import net.pantasystem.milktea.common_compose.MilkteaStyleConfigApplyAndTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import net.pantasystem.milktea.model.setting.LocalConfigRepository
import net.pantasystem.milktea.common.ui.ApplyTheme
import net.pantasystem.milktea.setting.R
import net.pantasystem.milktea.setting.compose.SettingTitleTile
import net.pantasystem.milktea.setting.viewmodel.CacheSettingViewModel
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class CacheSettingActivity : AppCompatActivity() {

    @Inject
    internal lateinit var applyTheme: ApplyTheme

    @Inject
    internal lateinit var configRepository: LocalConfigRepository

    private val viewModel by viewModels<CacheSettingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MilkteaStyleConfigApplyAndTheme(configRepository = configRepository) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                }
                            },
                            title = {
                                Text(stringResource(id = R.string.settings_cache_config))
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        Modifier
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SettingTitleTile(stringResource(id = R.string.settings_note_cache))
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("Size: ${uiState.noteCacheSize}")
                            TextButton(onClick = viewModel::onClearNoteCache) {
                                Text(stringResource(id = R.string.remove))
                            }
                        }

                        SettingTitleTile(stringResource(id = R.string.settings_custom_emoji_cache))
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("Size: ${uiState.imageCacheSize}")
                            TextButton(onClick = viewModel::onClearCustomEmojiCache) {
                                Text(stringResource(id = R.string.remove))
                            }
                        }

                        SettingTitleTile(stringResource(id = R.string.settings_user_cache))
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("Size: ${uiState.userCacheSize}")
                            TextButton(onClick = viewModel::onClearUserCache) {
                                Text(stringResource(id = R.string.remove))
                            }
                        }
                    }
                }
            }
        }
    }
}