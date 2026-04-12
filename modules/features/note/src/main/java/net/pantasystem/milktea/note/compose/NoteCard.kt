package net.pantasystem.milktea.note.compose

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import dagger.hilt.android.EntryPointAccessors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.pantasystem.milktea.common_android.platform.isWifiConnected
import net.pantasystem.milktea.common_android.resource.getString
import net.pantasystem.milktea.common_android_ui.BindingProvider
import net.pantasystem.milktea.common_android_ui.EmojiText
import net.pantasystem.milktea.common_android_ui.MfmText
import net.pantasystem.milktea.common_android_ui.TextType
import net.pantasystem.milktea.common_compose.rememberBlurhashPainter
import net.pantasystem.milktea.model.emoji.CustomEmoji
import net.pantasystem.milktea.model.note.Note
import net.pantasystem.milktea.model.note.poll.Poll
import net.pantasystem.milktea.model.note.reaction.Reaction
import net.pantasystem.milktea.model.user.User
import net.pantasystem.milktea.note.R
import net.pantasystem.milktea.note.media.viewmodel.MediaViewData
import net.pantasystem.milktea.note.media.viewmodel.PreviewAbleFile
import net.pantasystem.milktea.note.reaction.ImageAspectRatioCache
import net.pantasystem.milktea.note.reaction.ReactionViewData
import net.pantasystem.milktea.note.view.NoteCardAction
import net.pantasystem.milktea.note.viewmodel.NoteStatusMessageTextGenerator
import net.pantasystem.milktea.note.viewmodel.PlaneNoteViewData

/**
 * ノートカード（Renote / Reply ステータス行を含む）
 * item_note.xml に相当する Composable。
 */
@Composable
fun NoteCard(
    note: PlaneNoteViewData,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val statusMessage = remember(note.note) {
        NoteStatusMessageTextGenerator(note.note, isUserNameDefault = true)?.getString(context)
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            if (statusMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, top = 4.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SimpleNoteCardAsMain(note = note, onAction = onAction)
        }
    }
}

@Composable
fun SimpleNoteCardAsMain(
    note: PlaneNoteViewData,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentNote by note.currentNote.collectAsState()
    val isFolding by note.contentFolding.collectAsState()
    val mediaFiles by note.media.files.collectAsState()
    val reactions by note.reactionCountsViewData.collectAsState()
    val reactionCountsExpanded by note.reactionCountsExpanded.collectAsState()
    val isExpanded by note.expanded.collectAsState()
    val config by note.config.collectAsState()

    val paddingH = 12.dp
    val paddingV = 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingH, vertical = paddingV),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // AvatarIcon
            val avatarPlaceholder = rememberBlurhashPainter(note.toShowNote.user.avatarBlurhash)
            val instance = note.toShowNote.user.instance
            AsyncImage(
                model = note.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onAction(NoteCardAction.OnUserClicked(note.toShowNote.user)) },
                placeholder = avatarPlaceholder,
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.fillMaxWidth()
            ) {
                Header(
                    note = note,
                    userName = note.userName,
                    timestamp = note.toShowNote.note.createdAt,
                    modifier = Modifier.fillMaxWidth(),
                    onTimestampClick = {
                        onAction(
                            NoteCardAction.OnNoteCardClicked(
                                note.toShowNote.note,
                            )
                        )
                    }
                )
                if (config.isEnableInstanceTicker
                    && instance?.name != null
                    && instance.faviconUrl != null
                ) {
                    Spacer(Modifier.height(2.dp))
                    InstanceInfoLabel(
                        instance = instance,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AutoCollapsingLayout(
                    expanded = isExpanded,
                    onExpand = { note.expand() },
                ) {
                    Column {
                        NoteBodySection(
                            note = note,
                            isFolding = isFolding,
                            onToggleFolding = { note.changeContentFolding() },
                            onUrlClick = {
                                onAction(NoteCardAction.OnUrlClick(it))
                            },
                            onLinkClick = {
                                onAction(NoteCardAction.OnLinkClick(it))
                            },
                            onHashtagClick = {
                                onAction(NoteCardAction.OnHashtagClick(it))
                            },
                            onMentionClick = {
                                onAction(NoteCardAction.OnMentionClick(it))
                            },
                        )

                        val poll = currentNote.poll
                        if (!isFolding && poll != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            NotePollSection(
                                poll = poll,
                                noteId = currentNote.id,
                                onAction = onAction,
                            )
                        }

                        if (!isFolding && mediaFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            NoteMediaGrid(
                                mediaViewData = note.media,
                                files = mediaFiles,
                                onAction = onAction,
                            )
                        }

                        if (note.subNote != null && !isFolding) {
                            Spacer(modifier = Modifier.height(6.dp))
                            SubNoteCard(note = note, onAction = onAction)
                        }
                    }
                }

                val channel = note.channelInfo
                if (channel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteChannelChip(channel = channel, onAction = onAction)
                }

                if (reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteReactions(
                        note = note,
                        reactions = reactions,
                        expanded = reactionCountsExpanded,
                        onAction = onAction,
                    )
                }

                NoteActionBar(note = note, currentNote = currentNote, onAction = onAction)
            }
        }
    }
}

@Composable
private fun Header(
    note: PlaneNoteViewData,
    userName: String,
    timestamp: Instant,
    modifier: Modifier = Modifier,
    onTimestampClick: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically, // 垂直方向の中央揃えも追加しておくと綺麗です
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 1. 名前（長すぎる場合は省略）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            EmojiText(
                parsedResult = note.toShowNote.user.parsedResult,
                accountHost = note.account.getHost(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // 名前も長すぎると時間を圧迫するので、ここにもweightを入れるのが一般的です
//                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(Modifier.width(4.dp))

            // 2. ユーザー名（ここが可変領域）
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // fill = false を指定することで、中身が短いときはその分だけ、
                // 長いときは他の要素を押し出さない範囲で最大まで広がります
//                modifier = Modifier.weight(1f)
            )

        }
        Spacer(Modifier.width(8.dp))

        // 3. タイムスタンプ（絶対表示したい要素）
        val createdAtMs = timestamp.toEpochMilliseconds()
        Text(
            text = remember(createdAtMs) {
                DateUtils.getRelativeTimeSpanString(
                    createdAtMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                ).toString()
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.clickable {
                onTimestampClick()
            }
        )
    }
}

// ─────────────────────────────────────────────────
// NoteBodySection
// ─────────────────────────────────────────────────

@Composable
private fun NoteBodySection(
    note: PlaneNoteViewData,
    isFolding: Boolean,
    onToggleFolding: () -> Unit,
    onUrlClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
) {
    val cw = note.cw

    if (cw != null) {
        MfmText(
            text = cw,
            emojiNameMap = note.toShowNote.note.emojiNameMap ?: emptyMap(),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onToggleFolding) {
            Text(
                text = if (isFolding) stringResource(R.string.show)
                else stringResource(R.string.hide)
            )
        }
    }

    if (!isFolding) {
        NoteBodyText(
            note = note,
            onUrlClick = onUrlClick,
            onLinkClick = onLinkClick,
            onHashtagClick = onHashtagClick,
            onMentionClick = onMentionClick,
        )
    }
}

@Composable
private fun NoteBodyText(
    note: PlaneNoteViewData,
    onUrlClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
) {
    when (val textType = note.textNode) {
        is TextType.Misskey -> MfmText(
            nodes = textType.nodes,
            emojiNameMap = note.toShowNote.note.emojiNameMap ?: emptyMap(),
            style = MaterialTheme.typography.bodyMedium,
            onUrlClick = onUrlClick,
            onLinkClick = onLinkClick,
            onHashtagClick = onHashtagClick,
            onMentionClick = onMentionClick,
        )

        is TextType.Mastodon -> {
            val rawHtml = note.text
            if (!rawHtml.isNullOrBlank()) {
                // Mastodon HTML をプレーンテキストに変換して表示（書式は今後対応）
                val plainText = remember(rawHtml) {
                    HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                }
                Text(text = plainText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        null -> {
            val text = note.text
            if (!text.isNullOrBlank()) {
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────
// NoteMediaGrid
// MediaLayout と同じレイアウトアルゴリズムを Compose で再現する。
// アスペクト比 16:10、2カラム固定、枚数制限なし。
//
// 配置ルール（MediaLayout と同一）:
//   N=1         : 全幅
//   N=2         : 左右各1枚、高さ = 全幅 / aspect
//   N=奇数(≥3) : 奇数インデックス + 最後のアイテム → 右カラム、残り → 左カラム
//   N=偶数(≥4) : 奇数インデックス → 右カラム、偶数インデックス → 左カラム
// ─────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun NoteMediaGrid(
    mediaViewData: MediaViewData,
    files: List<PreviewAbleFile>,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = files.size
    if (n == 0) return

    val gap = 4.dp          // MediaLayout の spaceMargin 相当
    val aspect = 16f / 10f  // MediaLayout: aspectRatio = 16.0 / 10.0

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalW = maxWidth

        // N=1: 全幅
        if (n == 1) {
            MediaItem(
                file = files[0],
                index = 0,
                files = files,
                mediaViewData = mediaViewData,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalW / aspect),
            )
            return@BoxWithConstraints
        }

        val halfW = (totalW - gap) / 2
        val isOdd = n % 2 == 1

        // MediaLayout の isRight ロジックと同一
        val leftFiles = mutableListOf<Pair<Int, PreviewAbleFile>>()
        val rightFiles = mutableListOf<Pair<Int, PreviewAbleFile>>()
        for (i in 0 until n) {
            val isRight = if (isOdd) {
                i == n - 1 || i % 2 == 1
            } else {
                i % 2 == 1
            }
            if (isRight) rightFiles.add(i to files[i])
            else leftFiles.add(i to files[i])
        }

        // MediaLayout と同じ高さ計算
        // N=2: totalH = totalW / aspect（全幅基準）
        // N≥3: totalH = max(左行数, 右行数) × (halfW / aspect)
        val totalH = if (n == 2) {
            totalW / aspect
        } else {
            (halfW / aspect) * maxOf(leftFiles.size, rightFiles.size)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalH),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(
                modifier = Modifier
                    .width(halfW)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                leftFiles.forEach { (idx, file) ->
                    MediaItem(
                        file = file,
                        index = idx,
                        files = files,
                        mediaViewData = mediaViewData,
                        onAction = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .width(halfW)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                rightFiles.forEach { (idx, file) ->
                    MediaItem(
                        file = file,
                        index = idx,
                        files = files,
                        mediaViewData = mediaViewData,
                        onAction = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaItem(
    file: PreviewAbleFile,
    index: Int,
    files: List<PreviewAbleFile>,
    mediaViewData: MediaViewData,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mediaDisplayMode = remember(mediaViewData.config) {
        mediaViewData.config?.mediaDisplayMode
            ?: net.pantasystem.milktea.model.setting.DefaultConfig.config.mediaDisplayMode
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                when {
                    file.visibleType == PreviewAbleFile.VisibleType.SensitiveHide ->
                        onAction(
                            NoteCardAction.OnSensitiveMediaPreviewClicked(
                                mediaViewData,
                                index
                            )
                        )

                    file.isHiding ->
                        mediaViewData.show(index)

                    else ->
                        onAction(
                            NoteCardAction.OnMediaPreviewClicked(
                                previewAbleFile = file,
                                files = files,
                                index = index,
                                thumbnailView = java.lang.ref.WeakReference(null),
                            )
                        )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val blurhashPainter = rememberBlurhashPainter(
            blurhash = file.source.blurhash,
            width = 64,
            height = 64,
        )

        if (file.isHiding && blurhashPainter != null) {
            // 隠し状態 + blurhash あり → blurhash のみ表示（サムネイル読み込みなし）
            Image(
                painter = blurhashPainter,
                contentDescription = file.source.comment,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // 通常表示 or blurhash なし → サムネイルを読み込む（隠し状態は薄く表示）
            val thumbnailUrl = file.source.thumbnailUrl ?: file.source.path
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = file.source.comment,
                modifier = Modifier.fillMaxSize(),
                placeholder = blurhashPainter,
                contentScale = ContentScale.Crop,
                alpha = if (file.isHiding) 0.25f else 1f,
            )
        }

        if (file.isHiding) {
            // 半透明スクリム
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
            )
            // 中央: アイコン + メッセージ
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (file.visibleType == PreviewAbleFile.VisibleType.SensitiveHide)
                            R.string.sensitive_content
                        else
                            R.string.notes_media_click_to_load_image
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (file.isVisiblePlayButton) {
            // 動画再生ボタン
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // 右上: 表示 / 非表示トグルボタン
        IconButton(
            onClick = {
                mediaViewData.toggleVisibility(
                    index = index,
                    isMobileNetwork = !context.isWifiConnected(),
                    mediaDisplayMode = mediaDisplayMode,
                )
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
        ) {
            Icon(
                painter = painterResource(
                    if (file.isHiding) R.drawable.ic_baseline_image_24
                    else R.drawable.ic_baseline_hide_image_24
                ),
                contentDescription = stringResource(
                    if (file.isHiding) R.string.show else R.string.hide
                ),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────
// SubNoteCard (引用 Renote)
// ─────────────────────────────────────────────────

@Composable
private fun SubNoteCard(
    note: PlaneNoteViewData,
    onAction: (NoteCardAction) -> Unit,
) {
    val subNote = note.subNote ?: return
    val isSubFolding by note.subContentFolding.collectAsState()
    val subMediaFiles by note.subNoteMedia.files.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val subAvatarPlaceholder = rememberBlurhashPainter(subNote.user.avatarBlurhash)
                AsyncImage(
                    model = subNote.user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    placeholder = subAvatarPlaceholder,
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subNote.user.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val subCw = note.subCw
            if (subCw != null) {
                MfmText(
                    text = subCw,
                    emojiNameMap = subNote.note.emojiNameMap ?: emptyMap(),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { note.changeSubContentFolding() }) {
                    Text(
                        text = if (isSubFolding) stringResource(R.string.show)
                        else stringResource(R.string.hide),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            if (!isSubFolding) {
                when (val textType = note.subNoteTextNode) {
                    is TextType.Misskey -> MfmText(
                        nodes = textType.nodes,
                        emojiNameMap = subNote.note.emojiNameMap ?: emptyMap(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )

                    is TextType.Mastodon -> {
                        val rawHtml = subNote.note.text
                        if (!rawHtml.isNullOrBlank()) {
                            val plainText = remember(rawHtml) {
                                HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
                                    .toString()
                            }
                            Text(
                                text = plainText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    null -> {
                        val text = subNote.note.text
                        if (!text.isNullOrBlank()) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (subMediaFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteMediaGrid(
                        mediaViewData = note.subNoteMedia,
                        files = subMediaFiles,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
// NoteChannelChip
// ─────────────────────────────────────────────────

@Composable
private fun NoteChannelChip(
    channel: Note.Type.Misskey.SimpleChannelInfo,
    onAction: (NoteCardAction) -> Unit,
) {
    OutlinedButton(
        onClick = { onAction(NoteCardAction.OnChannelButtonClicked(channel.id)) },
        shape = RoundedCornerShape(50),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        modifier = Modifier.height(28.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Tag,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────
// NoteReactions
// ─────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteReactions(
    note: PlaneNoteViewData,
    reactions: List<ReactionViewData>,
    expanded: Boolean,
    onAction: (NoteCardAction) -> Unit,
) {
    val isMisskey = note.toShowNote.note.isMisskey
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { reaction ->
            ReactionChip(
                reaction = reaction,
                isReactionable = !isMisskey || Reaction(reaction.reaction).isLocal(),
                onClicked = { onAction(NoteCardAction.OnReactionClicked(note, reaction.reaction)) },
            )
        }

        if (!expanded) {
            TextButton(
                onClick = { note.expandReactions() },
                modifier = Modifier.height(32.dp),
            ) {
                Text(text = "…", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ReactionChip(
    reaction: ReactionViewData,
    isReactionable: Boolean,
    onClicked: () -> Unit,
) {
    val isMyReaction = reaction.isMyReaction
    val emoji: CustomEmoji? = reaction.emoji

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClicked),
        shape = RoundedCornerShape(50),
        color = if (isMyReaction) MaterialTheme.colorScheme.primaryContainer
        else if (!isReactionable) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji != null) {
                val context = LocalContext.current
                AsyncImage(
                    model = emoji.url ?: emoji.uri,
                    contentDescription = reaction.reaction,
                    modifier = Modifier
                        .height(20.dp)
                        .aspectRatio(reaction.emoji?.aspectRatio ?: 1f),
                    onSuccess = { state ->
                        val drawable = state.result.drawable
                        val imageAspectRatio =
                            drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                        val ep = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            BindingProvider::class.java,
                        )
                        ep.customEmojiAspectRatioStore().save(emoji, imageAspectRatio)
                        if (emoji.aspectRatio == null || emoji.aspectRatio != imageAspectRatio) {
                            ImageAspectRatioCache.put(emoji.url ?: emoji.uri, imageAspectRatio)
                        }
                        ep.emojiImageCacheStore().save(emoji)
                    },
                )
            } else {
                Text(
                    text = reaction.reaction,
                    style = LocalTextStyle.current.copy(fontSize = 14.sp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = reaction.reactionCount.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isMyReaction) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────
// NoteActionBar
// ─────────────────────────────────────────────────

@Composable
private fun NoteActionBar(
    note: PlaneNoteViewData,
    currentNote: Note,
    onAction: (NoteCardAction) -> Unit,
) {
    val favoriteCount by note.favoriteCount.collectAsState()
    val isMastodon = currentNote.type is Note.Type.Mastodon

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoteActionButton(
            icon = { Icon(Icons.Default.Reply, contentDescription = null) },
            count = currentNote.repliesCount.takeIf { it > 0 },
            onClick = { onAction(NoteCardAction.OnReplyButtonClicked(note)) },
        )

        NoteActionButton(
            icon = { Icon(Icons.Default.Repeat, contentDescription = null) },
            count = currentNote.renoteCount.takeIf { it > 0 },
            onClick = { onAction(NoteCardAction.OnRenoteButtonClicked(note)) },
        )

        if (isMastodon) {
            val isFavorited = (currentNote.type as? Note.Type.Mastodon)?.favorited == true
            NoteActionButton(
                icon = {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorited) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                count = favoriteCount?.takeIf { it > 0 },
                onClick = { onAction(NoteCardAction.OnFavoriteButtonClicked(currentNote)) },
            )
        } else {
            NoteActionButton(
                icon = { Icon(Icons.Default.SentimentSatisfied, contentDescription = null) },
                count = null,
                onClick = { onAction(NoteCardAction.OnReactionButtonClicked(note)) },
            )
        }

        IconButton(
            onClick = { onAction(NoteCardAction.OnOptionButtonClicked(note)) },
            modifier = Modifier.size(36.dp).padding(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteActionButton(
    icon: @Composable () -> Unit,
    count: Int?,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp).padding(6.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────
// NotePollSection
// ─────────────────────────────────────────────────

/**
 * 投票セクション。
 * - 投票可能 (poll.canVote) な選択肢はタップで投票できる。
 * - 投票済み / 期限切れの場合は結果バーのみ表示する。
 * - 複数選択 (poll.multiple) の場合は未投票の選択肢を個別にタップできる。
 */
@Composable
private fun NotePollSection(
    poll: Poll,
    noteId: Note.Id,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalVotes = poll.totalVoteCount

    Column(modifier = modifier.fillMaxWidth()) {
        poll.choices.forEach { choice ->
            val fraction = if (totalVotes > 0) choice.votes.toFloat() / totalVotes else 0f
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                label = "poll_bar_${choice.index}",
            )
            // 複数選択は未投票のものに限り個別タップ可能、単一選択は canVote に従う
            val isClickable = poll.canVote && !choice.isVoted

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (isClickable) Modifier.clickable {
                            onAction(NoteCardAction.OnPollChoiceClicked(noteId, poll, choice))
                        } else Modifier
                    ),
            ) {
                // 票数に応じた色付き背景（プログレスバー代わり）
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .background(
                            if (choice.isVoted) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 投票状態アイコン
                    when {
                        choice.isVoted -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )

                        poll.canVote -> Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )

                        else -> Spacer(Modifier.size(16.dp))
                    }

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = choice.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (choice.isVoted) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(Modifier.width(8.dp))

                    val percentage = if (totalVotes > 0)
                        (choice.votes.toFloat() / totalVotes * 100 + 0.5f).toInt()
                    else 0
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        // フッター: 総投票数 ＋ 期限
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.poll_votes_count, totalVotes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val expiresAt = poll.expiresAt
            if (expiresAt != null) {
                val now = Clock.System.now()
                if (expiresAt <= now) {
                    Text(
                        text = stringResource(R.string.poll_ended),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.poll_expires_in,
                            DateUtils.getRelativeTimeSpanString(
                                expiresAt.toEpochMilliseconds(),
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                            ),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstanceInfoLabel(
    instance: User.InstanceInfo,
    modifier: Modifier = Modifier,
) {
    val bgAndText = remember(instance.themeColor) {
        instance.themeColor?.let { colorStr ->
            runCatching {
                val rawColor = android.graphics.Color.parseColor(colorStr)
                val alpha = (255 * 0.42f).toInt()
                val withAlpha = Color((alpha shl 24) or (rawColor and 0x00FFFFFF))
                val textColor =
                    if (ColorUtils.calculateLuminance(rawColor) < 0.5) Color.White else Color.Black
                withAlpha to textColor
            }.getOrNull()
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .then(bgAndText?.let { (bg, _) -> Modifier.background(bg) } ?: Modifier)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AsyncImage(
            model = instance.faviconUrl,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = instance.name ?: "",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = bgAndText?.second ?: Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
