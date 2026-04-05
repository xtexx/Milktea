package net.pantasystem.milktea.channel

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import net.pantasystem.milktea.common_compose.rememberFragment
import net.pantasystem.milktea.model.channel.Channel

@Composable
fun ChannelDetailScreen(
    onNavigateUp: () -> Unit,
    onNavigateNoteEditor: (Channel.Id) -> Unit,
    channelId: Channel.Id,
    channel: Channel?,
    fragmentManagerProvider: () -> FragmentManager,
    timelineFragmentProvider: () -> Fragment,
) {
    var container: FragmentContainerView? by remember {
        mutableStateOf(null)
    }
    val timelineFragment = rememberFragment(fragmentManager = fragmentManagerProvider()) {
        timelineFragmentProvider()
    }

    LaunchedEffect(Unit) {
        fragmentManagerProvider().beginTransaction()
            .replace(container!!.id , timelineFragment)
            .commitAllowingStateLoss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Text(channel?.name ?: "")
                },
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 0.dp,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateNoteEditor(channelId) }) {
                Icon(Icons.Default.Edit, contentDescription = null)
            }
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier.padding(paddingValues),
            factory = { context ->
                FragmentContainerView(context).also { container ->
                    container.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    container.id = View.generateViewId()
                }.also {
                    container = it
                }
        },)
    }
}