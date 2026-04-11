package net.pantasystem.milktea.setting.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import net.pantasystem.milktea.common.ui.ApplyTheme
import net.pantasystem.milktea.common_android.debug.DebugFeatureFlags
import net.pantasystem.milktea.common_compose.MilkteaStyleConfigApplyAndTheme
import net.pantasystem.milktea.model.setting.LocalConfigRepository
import net.pantasystem.milktea.setting.R
import net.pantasystem.milktea.setting.SettingSection
import net.pantasystem.milktea.setting.compose.SettingSwitchTile
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class DeveloperSettingActivity : AppCompatActivity() {

    @Inject
    lateinit var applyTheme: ApplyTheme

    @Inject
    lateinit var configRepository: LocalConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        enableEdgeToEdge()

        setContent {
            MilkteaStyleConfigApplyAndTheme(configRepository = configRepository) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                }
                            },
                            title = {
                                Text(stringResource(R.string.settings_developer_options))
                            },
                        )
                    },
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        val context = LocalContext.current

                        SettingSection(title = "Compose") {
                            var composeTimelineEnabled by remember {
                                mutableStateOf(DebugFeatureFlags.isComposeTimelineEnabled(context))
                            }
                            SettingSwitchTile(
                                checked = composeTimelineEnabled,
                                onChanged = { enabled ->
                                    DebugFeatureFlags.setComposeTimelineEnabled(context, enabled)
                                    composeTimelineEnabled = enabled
                                },
                                subtitle = {
                                    Text(
                                        text = stringResource(R.string.settings_dev_compose_timeline_description),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                            ) {
                                Text(stringResource(R.string.settings_dev_compose_timeline))
                            }
                        }
                    }
                }
            }
        }
    }
}
