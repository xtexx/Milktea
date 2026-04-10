
package net.pantasystem.milktea.note

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.pantasystem.milktea.app_store.account.AccountStore
import net.pantasystem.milktea.app_store.setting.SettingStore
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.common.ui.ApplyTheme
import net.pantasystem.milktea.common_android.platform.PermissionUtil
import net.pantasystem.milktea.common_android.ui.Activities
import net.pantasystem.milktea.common_android.ui.putActivity
import net.pantasystem.milktea.common_android_ui.account.viewmodel.AccountViewModel
import net.pantasystem.milktea.common_compose.MilkteaStyleConfigApplyAndTheme
import net.pantasystem.milktea.common_navigation.AuthorizationArgs
import net.pantasystem.milktea.common_navigation.AuthorizationNavigation
import net.pantasystem.milktea.common_navigation.ChangedDiffResult
import net.pantasystem.milktea.common_navigation.DriveNavigation
import net.pantasystem.milktea.common_navigation.DriveNavigationArgs
import net.pantasystem.milktea.common_navigation.EXTRA_SELECTED_FILE_PROPERTY_IDS
import net.pantasystem.milktea.common_navigation.MainNavigation
import net.pantasystem.milktea.common_navigation.MediaNavigation
import net.pantasystem.milktea.common_navigation.MediaNavigationArgs
import net.pantasystem.milktea.common_navigation.SearchAndSelectUserNavigation
import net.pantasystem.milktea.common_navigation.SearchAndSelectUserNavigationArgs
import net.pantasystem.milktea.common_navigation.UserDetailNavigation
import net.pantasystem.milktea.common_navigation.UserDetailNavigationArgs
import net.pantasystem.milktea.model.channel.Channel
import net.pantasystem.milktea.model.drive.FileProperty
import net.pantasystem.milktea.model.emoji.CustomEmoji
import net.pantasystem.milktea.model.emoji.CustomEmojiRepository
import net.pantasystem.milktea.model.note.Note
import net.pantasystem.milktea.model.setting.LocalConfigRepository
import net.pantasystem.milktea.model.user.User
import net.pantasystem.milktea.note.editor.ConfirmSaveAsDraftDialog
import net.pantasystem.milktea.note.editor.NoteEditorFileSizeWarningDialog
import net.pantasystem.milktea.note.editor.NoteEditorScreen
import net.pantasystem.milktea.note.editor.ReservationPostDatePickerDialog
import net.pantasystem.milktea.note.editor.ReservationPostTimePickerDialog
import net.pantasystem.milktea.note.editor.account.NoteEditorSwitchAccountDialog
import net.pantasystem.milktea.note.editor.file.EditFileCaptionDialog
import net.pantasystem.milktea.note.editor.file.EditFileNameDialog
import net.pantasystem.milktea.note.editor.poll.PollDatePickerDialog
import net.pantasystem.milktea.note.editor.poll.PollTimePickerDialog
import net.pantasystem.milktea.note.editor.viewmodel.NoteEditorFocusEditTextType
import net.pantasystem.milktea.note.editor.viewmodel.NoteEditorSavedStateKey
import net.pantasystem.milktea.note.editor.viewmodel.NoteEditorViewModel
import net.pantasystem.milktea.note.editor.viewmodel.TextWithCursorPos
import net.pantasystem.milktea.note.editor.visibility.VisibilitySelectionDialogV2
import net.pantasystem.milktea.note.emojis.CustomEmojiPickerDialog
import net.pantasystem.milktea.note.emojis.viewmodel.EmojiSelection
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class NoteEditorActivity : AppCompatActivity(), EmojiSelection {

    companion object {
        private const val EXTRA_REPLY_TO_NOTE_ID =
            "jp.panta.misskeyandroidclient.EXTRA_REPLY_TO_NOTE_ID"
        private const val EXTRA_QUOTE_TO_NOTE_ID =
            "jp.panta.misskeyandroidclient.EXTRA_QUOTE_TO_NOTE_ID"
        private const val EXTRA_DRAFT_NOTE_ID = "jp.panta.misskeyandroidclient.EXTRA_DRAFT_NOTE"
        private const val EXTRA_ACCOUNT_ID = "jp.panta.misskeyandroidclient.EXTRA_ACCOUNT_ID"
        private const val EXTRA_MENTIONS = "EXTRA_MENTIONS"
        private const val EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID"
        private const val EXTRA_SPECIFIED_ACCOUNT_ID = "EXTRA_SPECIFIED_ACCOUNT_ID"

        fun newBundle(
            context: Context,
            replyTo: Note.Id? = null,
            quoteTo: Note.Id? = null,
            draftNoteId: Long? = null,
            mentions: List<String>? = null,
            channelId: Channel.Id? = null,
            accountId: Long? = null,
            text: String? = null,
        ): Intent {
            return Intent(context, NoteEditorActivity::class.java).apply {
                replyTo?.let {
                    putExtra(EXTRA_REPLY_TO_NOTE_ID, replyTo.noteId)
                    putExtra(EXTRA_ACCOUNT_ID, replyTo.accountId)
                }
                quoteTo?.let {
                    putExtra(EXTRA_QUOTE_TO_NOTE_ID, quoteTo.noteId)
                    putExtra(EXTRA_ACCOUNT_ID, quoteTo.accountId)
                }
                draftNoteId?.let {
                    putExtra(EXTRA_DRAFT_NOTE_ID, it)
                }
                mentions?.let {
                    putExtra(EXTRA_MENTIONS, it.toTypedArray())
                }
                channelId?.let {
                    putExtra(EXTRA_CHANNEL_ID, it.channelId)
                    putExtra(EXTRA_ACCOUNT_ID, it.accountId)
                }
                accountId?.let {
                    putExtra(EXTRA_SPECIFIED_ACCOUNT_ID, it)
                }
                text?.let {
                    putExtra(NoteEditorSavedStateKey.Text.name, it)
                }
            }
        }
    }

    val mViewModel: NoteEditorViewModel by viewModels()
    private val accountViewModel: AccountViewModel by viewModels()

    @Inject internal lateinit var applyTheme: ApplyTheme
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var settingStore: SettingStore
    @Inject internal lateinit var driveNavigation: DriveNavigation
    @Inject internal lateinit var authorizationNavigation: AuthorizationNavigation
    @Inject lateinit var mediaNavigation: MediaNavigation
    @Inject lateinit var searchAndUserNavigation: SearchAndSelectUserNavigation
    @Inject lateinit var mainNavigation: MainNavigation
    @Inject lateinit var userDetailNavigation: UserDetailNavigation
    @Inject lateinit var loggerFactory: Logger.Factory
    @Inject internal lateinit var configRepository: LocalConfigRepository
    @Inject internal lateinit var customEmojiRepository: CustomEmojiRepository

    private val logger by lazy { loggerFactory.create("NoteEditorActivity") }

    // テキストフィールドのカーソル位置（絵文字・メンション挿入位置の決定に使う）
    private var textCursorPosition: Int = 0
    private var cwCursorPosition: Int = 0

    private val accountId: Long? by lazy(LazyThreadSafetyMode.NONE) {
        intent.getLongExtra(EXTRA_ACCOUNT_ID, -1).takeIf { it != -1L }
    }
    private val replyToNoteId by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(EXTRA_REPLY_TO_NOTE_ID)?.let {
            Note.Id(requireNotNull(accountId), it)
        }
    }
    private val quoteToNoteId by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(EXTRA_QUOTE_TO_NOTE_ID)?.let {
            Note.Id(requireNotNull(accountId), it)
        }
    }
    private val channelId by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(EXTRA_CHANNEL_ID)?.let {
            Channel.Id(requireNotNull(accountId), it)
        }
    }
    private val draftNoteId by lazy(LazyThreadSafetyMode.NONE) {
        intent.getLongExtra(EXTRA_DRAFT_NOTE_ID, -1).takeIf { it != -1L }
    }
    private val mentions by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringArrayExtra(EXTRA_MENTIONS)?.toList()
    }
    private val specifiedAccountId by lazy(LazyThreadSafetyMode.NONE) {
        intent.getLongExtra(EXTRA_SPECIFIED_ACCOUNT_ID, -1).takeIf { it > 0 }
    }

    // EmojiSelection: CustomEmojiPickerDialog からのコールバック
    override fun onSelect(emoji: CustomEmoji) = onSelect(":${emoji.name}:")

    override fun onSelect(emoji: String) {
        when (mViewModel.focusType) {
            NoteEditorFocusEditTextType.Cw -> {
                mViewModel.addEmoji(emoji, cwCursorPosition)
            }
            NoteEditorFocusEditTextType.Text -> {
                val newPos = mViewModel.addEmoji(emoji, textCursorPosition)
                mViewModel.textCursorPos.tryEmit(
                    TextWithCursorPos(mViewModel.text.value, newPos)
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyTheme()

        // ACTION_SEND で共有されたコンテンツの処理
        var sharedText: String? = null
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                }
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                }
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE &&
                    intent.type?.startsWith("image/") == true -> {
                handleSendImages(intent)
            }
        }

        // ViewModel の初期化（savedInstanceState == null のときのみ）
        if (savedInstanceState == null) {
            mViewModel.setReplyTo(replyToNoteId)
            mViewModel.setRenoteTo(quoteToNoteId)
            if (channelId != null) mViewModel.setChannelId(channelId)
            if (draftNoteId != null) mViewModel.setDraftNoteId(requireNotNull(draftNoteId))
            mViewModel.setAccountId(specifiedAccountId)
            if (!sharedText.isNullOrBlank()) mViewModel.changeText(sharedText)
            if (!mentions.isNullOrEmpty()) addMentionUserNames(requireNotNull(mentions))
        }

        setupSideEffects()

        setContent {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            MilkteaStyleConfigApplyAndTheme(configRepository = configRepository) {
                NoteEditorScreen(
                    viewModel = mViewModel,
                    accountViewModel = accountViewModel,
                    customEmojiRepository = customEmojiRepository,
                    isPostButtonAtTheBottom = settingStore.isPostButtonAtTheBottom,
                    onNavigateUp = ::upTo,
                    onPickFileFromDrive = ::showDriveFileSelector,
                    onPickFileFromLocal = ::showFileManager,
                    onPickImageFromLocal = ::showMultipleImagePicker,
                    onSelectMentionUsers = ::startMentionToSearchAndSelectUser,
                    onSelectAddressUsers = ::startSearchAndSelectUser,
                    onShowEmojiPicker = {
                        CustomEmojiPickerDialog.newInstance(
                            mViewModel.currentAccount.value?.accountId
                        ).show(supportFragmentManager, CustomEmojiPickerDialog.FRAGMENT_TAG)
                    },
                    onShowDraftPicker = {
                        pickDraftNoteActivityResult.launch(
                            Intent(this, DraftNotesActivity::class.java).apply {
                                action = Intent.ACTION_PICK
                            }
                        )
                    },
                    onShowAlarmPermissionDescriptionDialogIfPermissionDenied = {
                        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                !alarmManager.canScheduleExactAlarms()
                            } else {
                                // TODO: 正しい実装をする
                                return@NoteEditorScreen false
                            }
                        ) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.alarm_permission_description_title)
                                .setMessage(R.string.alarm_permission_description_message)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                .show()
                            return@NoteEditorScreen true
                        }
                        false
                    },
                    onShowMediaPreview = {
                        startActivity(mediaNavigation.newIntent(MediaNavigationArgs.AFile(it)))
                    },
                    onEditFileCaption = {
                        EditFileCaptionDialog.newInstance(it.file, it.comment ?: "")
                            .show(supportFragmentManager, EditFileCaptionDialog.FRAGMENT_TAG)
                    },
                    onEditFileName = {
                        EditFileNameDialog.newInstance(it.file, it.name)
                            .show(supportFragmentManager, EditFileNameDialog.FRAGMENT_TAG)
                    },
                    onShowVisibilityDialog = {
                        VisibilitySelectionDialogV2()
                            .show(supportFragmentManager, VisibilitySelectionDialogV2.FRAGMENT_TAG)
                    },
                    onShowReservationDatePicker = {
                        ReservationPostDatePickerDialog()
                            .show(
                                supportFragmentManager,
                                ReservationPostDatePickerDialog.FRAGMENT_TAG
                            )
                    },
                    onShowReservationTimePicker = {
                        ReservationPostTimePickerDialog()
                            .show(
                                supportFragmentManager,
                                ReservationPostTimePickerDialog.FRAGMENT_TAG
                            )
                    },
                    onShowPollDatePicker = {
                        PollDatePickerDialog()
                            .show(supportFragmentManager, PollDatePickerDialog.FRAGMENT_TAG)
                    },
                    onShowPollTimePicker = {
                        PollTimePickerDialog()
                            .show(supportFragmentManager, PollTimePickerDialog.FRAGMENT_TAG)
                    },
                    onShowConfirmSaveAsDraftDialog = {
                        if (supportFragmentManager.findFragmentByTag(ConfirmSaveAsDraftDialog.FRAGMENT_TAG) == null) {
                            ConfirmSaveAsDraftDialog()
                                .show(
                                    supportFragmentManager,
                                    ConfirmSaveAsDraftDialog.FRAGMENT_TAG
                                )
                        }
                    },
                    onTextCursorPositionChanged = { textCursorPosition = it },
                    onCwCursorPositionChanged = { cwCursorPosition = it },
                )
            }
        }
    }

    // ---- サイドエフェクト ----

    private fun setupSideEffects() {
        // アカウント切り替えダイアログ
        accountViewModel.switchAccountEvent.onEach {
            NoteEditorSwitchAccountDialog()
                .show(supportFragmentManager, NoteEditorSwitchAccountDialog.FRAGMENT_TAG)
        }.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .launchIn(lifecycleScope)

        // ユーザー詳細への遷移
        accountViewModel.showProfileEvent.onEach {
            val intent = userDetailNavigation.newIntent(
                UserDetailNavigationArgs.UserId(User.Id(it.accountId, it.remoteId))
            )
            intent.putActivity(Activities.ACTIVITY_IN_APP)
            startActivity(intent)
        }.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .launchIn(lifecycleScope)

        // 未認証状態の検知
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountStore.state.collect {
                    if (it.isUnauthorized) {
                        finish()
                        startActivity(authorizationNavigation.newIntent(AuthorizationArgs.New))
                    }
                }
            }
        }

        // ファイルサイズ超過ダイアログ
        mViewModel.fileSizeInvalidEvent
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                NoteEditorFileSizeWarningDialog.newInstance(
                    it.account.getHost(),
                    it.instanceInfo.clientMaxBodyByteSize ?: 0,
                    it.file
                ).show(supportFragmentManager, NoteEditorFileSizeWarningDialog.FRAGMENT_TAG)
            }.launchIn(lifecycleScope)
    }

    // ---- ファイル操作 ----

    private fun showDriveFileSelector() {
        val selectedSize = mViewModel.uiState.value.totalFilesCount
        val selectableMaxSize = mViewModel.maxFileCount.value - selectedSize
        val intent = driveNavigation.newIntent(
            DriveNavigationArgs(
                selectableFileMaxSize = selectableMaxSize,
                accountId = mViewModel.currentAccount.value?.accountId,
            )
        ).apply { action = Intent.ACTION_OPEN_DOCUMENT }
        openDriveActivityResult.launch(intent)
    }

    private fun checkPermission(): Boolean = PermissionUtil.checkReadStoragePermission(this)

    private fun requestPermission() {
        if (!checkPermission()) {
            if (Build.VERSION.SDK_INT >= 33) {
                requestReadMediasPermissionResult.launch(
                    PermissionUtil.getReadMediaPermissions().toTypedArray()
                )
            } else {
                requestReadStoragePermissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showFileManager() {
        if (checkPermission()) {
            openLocalStorageResult.launch(arrayOf("*/*"))
        } else {
            requestPermission()
        }
    }

    private fun showMultipleImagePicker() {
        if (checkPermission()) {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable()) {
                pickMultipleMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            } else {
                openLocalStorageResult.launch(arrayOf("image/*", "video/*"))
            }
        } else {
            requestPermission()
        }
    }

    private fun appendFile(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        mViewModel.addFile(uri)
    }

    @Suppress("DEPRECATION")
    private fun handleSendImage(intent: Intent) {
        (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { appendFile(it) }
    }

    @Suppress("DEPRECATION")
    private fun handleSendImages(intent: Intent) {
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
            ?.mapNotNull { it as? Uri }
            ?.distinct()
            ?.forEach { appendFile(it) }
    }

    // ---- ユーザー選択 ----

    private fun startSearchAndSelectUser() {
        val selectedUserIds = mViewModel.address.value.mapNotNull { it.userId }
        val intent = searchAndUserNavigation.newIntent(
            SearchAndSelectUserNavigationArgs(
                selectedUserIds = selectedUserIds,
                accountId = mViewModel.currentAccount.value?.accountId,
            )
        )
        selectUserResult.launch(intent)
    }

    private fun startMentionToSearchAndSelectUser() {
        val intent = searchAndUserNavigation.newIntent(
            SearchAndSelectUserNavigationArgs(
                accountId = mViewModel.currentAccount.value?.accountId,
            )
        )
        selectMentionToUserResult.launch(intent)
    }

    private fun addMentionUserNames(userNames: List<String>) {
        val newPos = mViewModel.addMentionUserNames(userNames, textCursorPosition)
        mViewModel.textCursorPos.tryEmit(
            TextWithCursorPos(mViewModel.text.value, newPos)
        )
    }

    // ---- ナビゲーション ----

    private fun upTo() {
        val initialText = intent.getStringExtra(NoteEditorSavedStateKey.Text.name)
        if (initialText.isNullOrEmpty()) {
            finish()
        } else {
            val upIntent = mainNavigation.newIntent(Unit)
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (shouldUpRecreateTask(upIntent)) {
                TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities()
                finish()
            } else {
                navigateUpTo(upIntent)
            }
        }
    }

    // ---- ActivityResultLaunchers ----

    private val openDriveActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val ids = (result?.data?.getSerializableExtra(EXTRA_SELECTED_FILE_PROPERTY_IDS) as List<*>?)
            ?.mapNotNull { it as? FileProperty.Id }
        logger.debug("result:$ids")
        val size = mViewModel.fileTotal()
        if (!ids.isNullOrEmpty() && size + ids.size <= mViewModel.maxFileCount.value) {
            mViewModel.addFilePropertyFromIds(ids)
        }
    }

    private val pickDraftNoteActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val draftNoteId = result.data
            ?.getLongExtra(DraftNotesActivity.EXTRA_DRAFT_NOTE_ID, -1)
            ?.takeIf { it > 0L }
        if (draftNoteId != null) {
            mViewModel.setDraftNoteId(draftNoteId)
        }
    }

    private val openLocalStorageResult =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            uris?.forEach { appendFile(it) }
        }

    private val requestReadStoragePermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) showFileManager()
        else Toast.makeText(
            this, "ストレージへのアクセスを許可しないとファイルを読み込めないぽよ", Toast.LENGTH_LONG
        ).show()
    }

    private val requestReadMediasPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.any { it.value }) showFileManager()
        else Toast.makeText(
            this, "ストレージへのアクセスを許可しないとファイルを読み込めないぽよ", Toast.LENGTH_LONG
        ).show()
    }

    private val selectUserResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val changed = result.data
                ?.getSerializableExtra(SearchAndSelectUserNavigation.EXTRA_SELECTED_USER_CHANGED_DIFF)
                as? ChangedDiffResult
            if (changed != null) {
                mViewModel.setAddress(changed.added, changed.removed)
            }
        }
    }

    private val selectMentionToUserResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val changed = result.data
                ?.getSerializableExtra(SearchAndSelectUserNavigation.EXTRA_SELECTED_USER_CHANGED_DIFF)
                as? ChangedDiffResult
            if (changed != null) {
                addMentionUserNames(changed.selectedUserNames)
            }
        }
    }

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris?.forEach { appendFile(it) }
    }
}
