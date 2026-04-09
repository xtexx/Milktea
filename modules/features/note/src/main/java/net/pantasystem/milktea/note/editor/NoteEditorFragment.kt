package net.pantasystem.milktea.note.editor

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.pantasystem.milktea.app_store.account.AccountStore
import net.pantasystem.milktea.app_store.setting.SettingStore
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.common_android.platform.PermissionUtil
import net.pantasystem.milktea.common_android.ui.Activities
import net.pantasystem.milktea.common_android.ui.putActivity
import net.pantasystem.milktea.common_android_ui.account.viewmodel.AccountViewModel
import net.pantasystem.milktea.common_compose.MilkteaStyleConfigApplyAndTheme
import net.pantasystem.milktea.common_navigation.AuthorizationArgs
import net.pantasystem.milktea.common_navigation.AuthorizationNavigation
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
import net.pantasystem.milktea.note.DraftNotesActivity
import net.pantasystem.milktea.note.R
import net.pantasystem.milktea.note.editor.account.NoteEditorSwitchAccountDialog
import net.pantasystem.milktea.note.editor.file.EditFileCaptionDialog
import net.pantasystem.milktea.note.editor.file.EditFileNameDialog
import net.pantasystem.milktea.note.editor.poll.PollDatePickerDialog
import net.pantasystem.milktea.note.editor.poll.PollTimePickerDialog
import net.pantasystem.milktea.note.editor.viewmodel.NoteEditorFocusEditTextType
import net.pantasystem.milktea.note.editor.viewmodel.NoteEditorViewModel
import net.pantasystem.milktea.note.editor.viewmodel.TextWithCursorPos
import net.pantasystem.milktea.note.editor.visibility.VisibilitySelectionDialogV2
import net.pantasystem.milktea.note.emojis.CustomEmojiPickerDialog
import net.pantasystem.milktea.note.emojis.viewmodel.EmojiSelection
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class NoteEditorFragment : Fragment(), EmojiSelection {

    companion object {
        private const val EXTRA_REPLY_TO_NOTE_ID = "EXTRA_REPLY_TO_NOTE_ID"
        private const val EXTRA_QUOTE_TO_NOTE_ID = "EXTRA_QUOTE_TO_NOTE_ID"
        private const val EXTRA_DRAFT_NOTE_ID = "EXTRA_DRAFT_NOTE"
        private const val EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID"
        private const val EXTRA_MENTIONS = "EXTRA_MENTIONS"
        private const val EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID"
        private const val EXTRA_TEXT = "EXTRA_TEXT"
        private const val EXTRA_SPECIFIED_ACCOUNT_ID = "EXTRA_SPECIFIED_ACCOUNT_ID"

        fun newInstance(
            replyTo: Note.Id? = null,
            quoteTo: Note.Id? = null,
            draftNoteId: Long? = null,
            mentions: List<String>? = null,
            channelId: Channel.Id? = null,
            text: String? = null,
            specifiedAccountId: Long? = null,
        ): NoteEditorFragment {
            return NoteEditorFragment().apply {
                arguments = Bundle().apply {
                    if (replyTo != null) {
                        putString(EXTRA_REPLY_TO_NOTE_ID, replyTo.noteId)
                        putLong(EXTRA_ACCOUNT_ID, replyTo.accountId)
                    }
                    if (quoteTo != null) {
                        putString(EXTRA_QUOTE_TO_NOTE_ID, quoteTo.noteId)
                        putLong(EXTRA_ACCOUNT_ID, quoteTo.accountId)
                    }
                    if (draftNoteId != null) {
                        putLong(EXTRA_DRAFT_NOTE_ID, draftNoteId)
                    }
                    if (mentions != null) {
                        putStringArray(EXTRA_MENTIONS, mentions.toTypedArray())
                    }
                    if (channelId != null) {
                        putString(EXTRA_CHANNEL_ID, channelId.channelId)
                        putLong(EXTRA_ACCOUNT_ID, channelId.accountId)
                    }
                    if (text != null) {
                        putString(EXTRA_TEXT, text)
                    }
                    if (specifiedAccountId != null) {
                        putLong(EXTRA_SPECIFIED_ACCOUNT_ID, specifiedAccountId)
                    }
                }
            }
        }
    }

    private val noteEditorViewModel: NoteEditorViewModel by activityViewModels()
    private val accountViewModel: AccountViewModel by activityViewModels()

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

    private val logger by lazy { loggerFactory.create("NoteEditorFragment") }

    // テキストフィールドのカーソル位置を追跡（絵文字・メンション挿入時の位置決定に使う）
    private val textCursorPosition = mutableIntStateOf(0)
    private val cwCursorPosition = mutableIntStateOf(0)

    private val accountId: Long? by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getLong(EXTRA_ACCOUNT_ID, -1).takeIf { it != -1L }
    }
    private val replyToNoteId by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getString(EXTRA_REPLY_TO_NOTE_ID)?.let {
            Note.Id(requireNotNull(accountId), it)
        }
    }
    private val quoteToNoteId by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getString(EXTRA_QUOTE_TO_NOTE_ID)?.let {
            Note.Id(requireNotNull(accountId), it)
        }
    }
    private val channelId by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getString(EXTRA_CHANNEL_ID)?.let {
            Channel.Id(requireNotNull(accountId), it)
        }
    }
    private val draftNoteId by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getLong(EXTRA_DRAFT_NOTE_ID, -1).takeIf { it != -1L }
    }
    private val mentions by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getStringArray(EXTRA_MENTIONS)?.toList()
    }
    private val text by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getString(EXTRA_TEXT, null)
    }
    private val specifiedAccountId by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getLong(EXTRA_SPECIFIED_ACCOUNT_ID, -1).takeIf { it > 0 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val alarmManager =
                requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            MilkteaStyleConfigApplyAndTheme(configRepository = configRepository) {
                NoteEditorScreen(
                    viewModel = noteEditorViewModel,
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
                            noteEditorViewModel.currentAccount.value?.accountId
                        ).show(childFragmentManager, CustomEmojiPickerDialog.FRAGMENT_TAG)
                    },
                    onShowDraftPicker = {
                        pickDraftNoteActivityResult.launch(
                            Intent(requireActivity(), DraftNotesActivity::class.java).apply {
                                action = Intent.ACTION_PICK
                            }
                        )
                    },
                    onShowAlarmPermissionDescriptionDialogIfPermissionDenied = {
                        if (!alarmManager.canScheduleExactAlarms()) {
                            MaterialAlertDialogBuilder(requireContext())
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
                        val intent = mediaNavigation.newIntent(MediaNavigationArgs.AFile(it))
                        requireActivity().startActivity(intent)
                    },
                    onEditFileCaption = {
                        EditFileCaptionDialog.newInstance(it.file, it.comment ?: "")
                            .show(childFragmentManager, EditFileCaptionDialog.FRAGMENT_TAG)
                    },
                    onEditFileName = {
                        EditFileNameDialog.newInstance(it.file, it.name)
                            .show(childFragmentManager, EditFileNameDialog.FRAGMENT_TAG)
                    },
                    onShowVisibilityDialog = {
                        VisibilitySelectionDialogV2()
                            .show(childFragmentManager, VisibilitySelectionDialogV2.FRAGMENT_TAG)
                    },
                    onShowReservationDatePicker = {
                        ReservationPostDatePickerDialog()
                            .show(childFragmentManager, ReservationPostDatePickerDialog.FRAGMENT_TAG)
                    },
                    onShowReservationTimePicker = {
                        ReservationPostTimePickerDialog()
                            .show(childFragmentManager, ReservationPostTimePickerDialog.FRAGMENT_TAG)
                    },
                    onShowPollDatePicker = {
                        PollDatePickerDialog()
                            .show(childFragmentManager, PollDatePickerDialog.FRAGMENT_TAG)
                    },
                    onShowPollTimePicker = {
                        PollTimePickerDialog()
                            .show(childFragmentManager, PollTimePickerDialog.FRAGMENT_TAG)
                    },
                    onShowConfirmSaveAsDraftDialog = {
                        if (childFragmentManager.findFragmentByTag(ConfirmSaveAsDraftDialog.FRAGMENT_TAG) == null) {
                            ConfirmSaveAsDraftDialog()
                                .show(childFragmentManager, ConfirmSaveAsDraftDialog.FRAGMENT_TAG)
                        }
                    },
                    onTextCursorPositionChanged = { textCursorPosition.intValue = it },
                    onCwCursorPositionChanged = { cwCursorPosition.intValue = it },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel の初期化（引数から状態を復元）
        noteEditorViewModel.setReplyTo(replyToNoteId)
        noteEditorViewModel.setRenoteTo(quoteToNoteId)
        if (channelId != null) {
            noteEditorViewModel.setChannelId(channelId)
        }
        if (draftNoteId != null && savedInstanceState == null) {
            noteEditorViewModel.setDraftNoteId(draftNoteId!!)
        }
        noteEditorViewModel.setAccountId(specifiedAccountId)
        if (!text.isNullOrBlank() && savedInstanceState == null) {
            noteEditorViewModel.changeText(text)
        }
        if (mentions != null && savedInstanceState == null) {
            addMentionUserNames(mentions!!)
        }

        // アカウント切り替えダイアログ
        accountViewModel.switchAccountEvent.onEach {
            NoteEditorSwitchAccountDialog()
                .show(childFragmentManager, NoteEditorSwitchAccountDialog.FRAGMENT_TAG)
        }.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // ユーザー詳細への遷移
        accountViewModel.showProfileEvent.onEach {
            val intent = userDetailNavigation.newIntent(
                UserDetailNavigationArgs.UserId(User.Id(it.accountId, it.remoteId))
            )
            intent.putActivity(Activities.ACTIVITY_IN_APP)
            startActivity(intent)
        }.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // 未認証状態の検知
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountStore.state.collect {
                    if (it.isUnauthorized) {
                        requireActivity().finish()
                        startActivity(authorizationNavigation.newIntent(AuthorizationArgs.New))
                    }
                }
            }
        }

        // ファイルサイズ超過ダイアログ
        viewLifecycleOwner.lifecycleScope.launch {
            noteEditorViewModel.fileSizeInvalidEvent.collect {
                whenStarted {
                    NoteEditorFileSizeWarningDialog.newInstance(
                        it.account.getHost(),
                        it.instanceInfo.clientMaxBodyByteSize ?: 0,
                        it.file
                    ).show(childFragmentManager, NoteEditorFileSizeWarningDialog.FRAGMENT_TAG)
                }
            }
        }
    }

    // EmojiSelection: CustomEmojiPickerDialog からのコールバック
    override fun onSelect(emoji: CustomEmoji) {
        onSelect(":${emoji.name}:")
    }

    override fun onSelect(emoji: String) {
        when (noteEditorViewModel.focusType) {
            NoteEditorFocusEditTextType.Cw -> {
                noteEditorViewModel.addEmoji(emoji, cwCursorPosition.intValue)
                // CW テキストは StateFlow → EmojiAutoCompleteTextField.update で反映される
                // カーソルは末尾になるが CW では許容範囲
            }
            NoteEditorFocusEditTextType.Text -> {
                val newPos = noteEditorViewModel.addEmoji(emoji, textCursorPosition.intValue)
                // textCursorPos に emit して EmojiAutoCompleteTextField の LaunchedEffect を起動
                noteEditorViewModel.textCursorPos.tryEmit(
                    TextWithCursorPos(noteEditorViewModel.text.value, newPos)
                )
            }
        }
    }

    // ---- ファイル操作 ----

    private fun showDriveFileSelector() {
        val selectedSize = noteEditorViewModel.uiState.value.totalFilesCount
        val selectableMaxSize = noteEditorViewModel.maxFileCount.value - selectedSize
        val intent = driveNavigation.newIntent(
            DriveNavigationArgs(
                selectableFileMaxSize = selectableMaxSize,
                accountId = noteEditorViewModel.currentAccount.value?.accountId,
            )
        ).apply { action = Intent.ACTION_OPEN_DOCUMENT }
        openDriveActivityResult.launch(intent)
    }

    private fun checkPermission(): Boolean =
        PermissionUtil.checkReadStoragePermission(requireContext())

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
        requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
        noteEditorViewModel.addFile(uri)
    }

    // ---- ユーザー選択 ----

    private fun startSearchAndSelectUser() {
        val selectedUserIds = noteEditorViewModel.address.value.mapNotNull { it.userId }
        val intent = searchAndUserNavigation.newIntent(
            SearchAndSelectUserNavigationArgs(
                selectedUserIds = selectedUserIds,
                accountId = noteEditorViewModel.currentAccount.value?.accountId,
            )
        )
        selectUserResult.launch(intent)
    }

    private fun startMentionToSearchAndSelectUser() {
        val intent = searchAndUserNavigation.newIntent(
            SearchAndSelectUserNavigationArgs(
                accountId = noteEditorViewModel.currentAccount.value?.accountId,
            )
        )
        selectMentionToUserResult.launch(intent)
    }

    private fun addMentionUserNames(userNames: List<String>) {
        val newPos = noteEditorViewModel.addMentionUserNames(userNames, textCursorPosition.intValue)
        noteEditorViewModel.textCursorPos.tryEmit(
            TextWithCursorPos(noteEditorViewModel.text.value, newPos)
        )
    }

    // ---- ナビゲーション ----

    private fun upTo() {
        if (text.isNullOrEmpty()) {
            requireActivity().finish()
        } else {
            val upIntent = mainNavigation.newIntent(Unit)
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (requireActivity().shouldUpRecreateTask(upIntent)) {
                TaskStackBuilder.create(requireActivity())
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities()
                requireActivity().finish()
            } else {
                requireActivity().navigateUpTo(upIntent)
            }
        }
    }

    // ---- ActivityResultLaunchers ----

    @Suppress("DEPRECATION")
    private val openDriveActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val ids = (result?.data?.getSerializableExtra(EXTRA_SELECTED_FILE_PROPERTY_IDS) as List<*>?)
            ?.mapNotNull { it as? FileProperty.Id }
        logger.debug("result:${ids}")
        val size = noteEditorViewModel.fileTotal()
        if (!ids.isNullOrEmpty() && size + ids.size <= noteEditorViewModel.maxFileCount.value) {
            noteEditorViewModel.addFilePropertyFromIds(ids)
        }
    }

    private val pickDraftNoteActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val draftNoteId = result.data
            ?.getLongExtra(DraftNotesActivity.EXTRA_DRAFT_NOTE_ID, -1)
            ?.takeIf { it > 0L }
        if (draftNoteId != null) {
            noteEditorViewModel.setDraftNoteId(draftNoteId)
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
            requireContext(), "ストレージへのアクセスを許可しないとファイルを読み込めないぽよ", Toast.LENGTH_LONG
        ).show()
    }

    private val requestReadMediasPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.any { it.value }) showFileManager()
        else Toast.makeText(
            requireContext(), "ストレージへのアクセスを許可しないとファイルを読み込めないぽよ", Toast.LENGTH_LONG
        ).show()
    }

    @Suppress("DEPRECATION")
    private val selectUserResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val changed = result.data
                ?.getSerializableExtra(SearchAndSelectUserNavigation.EXTRA_SELECTED_USER_CHANGED_DIFF)
                as? net.pantasystem.milktea.common_navigation.ChangedDiffResult
            if (changed != null) {
                noteEditorViewModel.setAddress(changed.added, changed.removed)
            }
        }
    }

    @Suppress("DEPRECATION")
    private val selectMentionToUserResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val changed = result.data
                ?.getSerializableExtra(SearchAndSelectUserNavigation.EXTRA_SELECTED_USER_CHANGED_DIFF)
                as? net.pantasystem.milktea.common_navigation.ChangedDiffResult
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
