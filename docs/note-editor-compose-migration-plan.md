# NoteEditorActivity Jetpack Compose 移行計画

このドキュメントは Claude が作業を進めるための実装計画書です。
完了したタスクは `- [ ]` を `- [x]` に変えてください。
各フェーズに着手する前に「前提条件」を確認し、完了後は「完了確認」を実施してください。

---

## 基本方針

`NoteEditorActivity` は現在、DataBinding を使う Fragment ベースのハイブリッド実装になっている。
ツールバー・ファイルプレビュー・ユーザーアクションメニュー・投票エディタなど約1,600行は既に Compose 化済み。
残りの XML/Fragment 部分（約2,100行）を段階的に置き換える。

**インクリメンタル戦略:** 内側から外側へ順に置き換え、各フェーズ終了後にビルド・動作確認できる状態を維持する。

---

## 現在のアーキテクチャ

```
NoteEditorActivity (AppCompatActivity + DataBinding)
  └── NoteEditorFragment (Fragment + DataBinding)
        ├── ComposeView: NoteEditorToolbar              ← 移行済み
        ├── RecyclerView: addressUsersView              ← 要移行
        ├── MultiAutoCompleteTextView: cw               ← 要移行（難）
        ├── MultiAutoCompleteTextView: inputMain        ← 要移行（難）
        ├── FrameLayout: edit_poll
        │     └── PollEditorFragment (Compose inside)  ← 移行済み
        ├── ComposeView: filePreview                    ← 移行済み
        ├── ComposeView: noteEditorUserActionMenu       ← 移行済み
        └── BottomSheetDialogFragment 群（多数）         ← 大半が移行済み
```

---

## フェーズ一覧

| フェーズ | 内容 | リスク | 完了後にリリース可能？ |
|---------|------|--------|----------------------|
| Phase 1 | Composable の骨格作成（未接続） | 低 | Yes |
| Phase 2 | `EmojiAutoCompleteTextField`（AndroidView ラッパー） | 中 | Yes |
| Phase 3 | `NoteEditorFragment` のレイアウトを Compose に置換 | 高 | Yes |
| Phase 4 | Fragment を削除し Activity を `setContent` に移行 | 中 | Yes |
| Phase 5 | ダイアログの純粋 Compose 化（任意） | 低 | Yes |

---

## Phase 1: Composable の骨格作成

**ゴール:** ViewModel に接続せず、UI 構造だけを Compose で作る。Preview で確認できる状態にする。

**前提条件:** なし（最初のフェーズ）

### 作成するファイル

#### `NoteEditorScreen.kt`
最上位の stateful Composable。状態収集・コールバック受け渡しの責務のみを持つ。

```kotlin
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateUp: () -> Unit,
    onPickFileFromDrive: () -> Unit,
    onPickFileFromLocal: () -> Unit,
    onPickImageFromLocal: () -> Unit,
    onSelectMentionUsers: () -> Unit,
    onSelectAddressUsers: () -> Unit,
    onShowEmojiPicker: () -> Unit,
    onShowDraftPicker: () -> Unit,
    onShowMediaPreview: (FilePreviewSource) -> Unit,
    onEditFileCaption: (FilePreviewSource) -> Unit,
    onEditFileName: (FilePreviewSource) -> Unit,
)
```

ViewModel から状態を collect し、子 Composable へ値として渡す。ダイアログのトリガーはラムダで上位に委譲する。

#### `NoteEditorTextInputSection.kt`
テキスト入力エリア全体（CW フィールド + 本文フィールド + 文字数カウント）。
Phase 2 で実装する `EmojiAutoCompleteTextField` を組み込む。

#### `NoteEditorAddressSection.kt`
宛先ユーザーチップ + 追加ボタン。`FlowRow` または `LazyRow` で実装。
`UserChipListAdapter` の RecyclerView を置き換える。

#### `NoteEditorReplyPreview.kt`
リプライ先ノートの簡易プレビューカード。
`item_note_editor_reply_to_note.xml` の代替。DataBinding の複雑なバインディングは不要で、
アバター・表示名・本文テキストだけを表示するシンプルな実装でよい。

#### `NoteEditorScheduleSection.kt`
予約投稿の日時表示 + クリア・編集ボタン行。

### 設計上の注意

- ViewModel のインスタンスを孫 Composable まで引き回さない。`NoteEditorScreen` で `uiState` を collect し、派生した値のみを子に渡す
- `NoteEditorUiState` は既によく整理された構造になっているため、そのまま活用できる

### チェックリスト

- [ ] `NoteEditorScreen.kt` を作成し、既存の Compose コンポーネント（`NoteEditorToolbar`、`NoteFilePreview`、`NoteEditorUserActionMenuLayout`）を組み込む
- [ ] `NoteEditorTextInputSection.kt` を作成（この時点では `AndroidView` なしのプレースホルダーでよい）
- [ ] `NoteEditorAddressSection.kt` を作成
- [ ] `NoteEditorReplyPreview.kt` を作成
- [ ] `NoteEditorScheduleSection.kt` を作成
- [ ] 各 Composable に `@Preview` を追加して確認
- [ ] `./gradlew :modules:features:note:compileDebugKotlin` でコンパイルエラーがないことを確認

---

## Phase 2: EmojiAutoCompleteTextField の実装

**ゴール:** `MultiAutoCompleteTextView` を `AndroidView` でラップした Composable を作る。

**前提条件:** Phase 1 完了

これが移行の最大難所。Compose の `TextField` / `BasicTextField` にはビルトインの補完機能がないため、
まず `AndroidView` でブリッジし、安定後に純粋 Compose 実装に移行する（別フェーズ）。

### `EmojiAutoCompleteTextField.kt`

```kotlin
@Composable
fun EmojiAutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit,
    account: Account?,
    customEmojiRepository: CustomEmojiRepository,
    modifier: Modifier = Modifier,
    hint: String = "",
    cursorPosition: Int? = null,  // ViewModel の textCursorPos flow から受け取る
)
```

実装方針:
- `AndroidView { MultiAutoCompleteTextView(ctx) }` で既存の `CustomEmojiCompleteAdapter` + `CustomEmojiTokenizer` をそのまま使う
- `update` ブロックで `value` の変化を TextView に反映する（ループ防止のため `text.toString() != value` でガード）
- `cursorPosition` が変化したら `setSelection()` を呼ぶ

### フォーカス管理の置き換え

現在 ViewModel に `var focusType: NoteEditorFocusEditTextType` という mutable var があり、Fragment がフォーカスイベントで書き換えている。
Compose では `NoteEditorScreen` のローカル状態として持つ：

```kotlin
var focusedField by remember { mutableStateOf(NoteEditorFocusEditTextType.Text) }
```

絵文字選択コールバックでこれを参照する。ViewModel の `focusType` は `SimpleEditorFragment` が移行されるまで残す。

### 注意事項

- `AndroidView` を `LazyColumn` の中に入れると再測定問題が起きる。テキスト入力エリアは `verticalScroll` + `Column` で実装する
- `MultiAutoCompleteTextView` の `performFiltering` 内に `runBlocking` があるが、これは既存の問題なので今回は触らない
- IME Insets は Activity 側の `enableEdgeToEdge()` + `ViewCompat.setOnApplyWindowInsetsListener` で既に処理済みなので競合に注意

### チェックリスト

- [ ] `EmojiAutoCompleteTextField.kt` を作成し、`AndroidView` で `MultiAutoCompleteTextView` をラップする
- [ ] `CustomEmojiCompleteAdapter` + `CustomEmojiTokenizer` を `AndroidView` の `factory` ブロックで設定する
- [ ] `update` ブロックでテキスト同期とカーソル位置同期を実装する
- [ ] `NoteEditorTextInputSection.kt` を `EmojiAutoCompleteTextField` を使う実装に更新する
- [ ] `NoteEditorScreen` 内に `focusedField` ローカル状態を追加する
- [ ] `./gradlew :modules:features:note:compileDebugKotlin` でコンパイルエラーがないことを確認

---

## Phase 3: NoteEditorFragment を Compose に置換

**ゴール:** `NoteEditorFragment` の DataBinding レイアウトを `NoteEditorScreen` に差し替える。Fragment と Activity は残す。

**前提条件:** Phase 1・2 完了。**このフェーズが最もリスクが高い。着手前に必ず現在の動作を手元で確認すること。**

### 変更手順

1. `NoteEditorFragment` の `Fragment(R.layout.fragment_note_editor)` を `Fragment()` に変更し、`onCreateView` で `ComposeView` を返す

```kotlin
override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
): View = ComposeView(requireContext()).apply {
    setContent {
        MilkteaStyleConfigApplyAndTheme(...) {
            NoteEditorScreen(
                viewModel = viewModel,
                onPickFileFromDrive = { openDriveLauncher.launch(...) },
                ...
            )
        }
    }
}
```

2. `ActivityResultContracts` のランチャーは Fragment に残す（ライフサイクルオーナーが必要なため）。ラムダとして `NoteEditorScreen` に渡す

3. `PollEditorFragment` のトランザクションを削除し、`PollEditorLayout` を `NoteEditorScreen` 内で直接呼ぶ（`uiState.poll != null` の条件分岐）

4. `ComposeView` の島（toolbar・filePreview・userActionMenu）を `NoteEditorScreen` 内に統合

5. `UserChipListAdapter` + RecyclerView を `NoteEditorAddressSection` に置換

### バックプレス処理

現在 `requireActivity().onBackPressedDispatcher.addCallback(this)` で下書き保存確認を行っている。
Fragment が残っている間はこのまま維持する。Phase 4 で `BackHandler { }` に移行する。

### `settingStore.isPostButtonAtTheBottom` の対応

投稿ボタン位置の切り替えロジックを `NoteEditorScreen` 内で Compose の条件分岐に置き換える：

```kotlin
if (uiState.isPostButtonAtTheBottom) {
    // アクションメニューを下部に配置
} else {
    // ツールバー内に配置
}
```

### チェックリスト

- [ ] `NoteEditorFragment.onCreateView` を `ComposeView` を返す実装に変更する
- [ ] DataBinding 関連コード（`binding.*` の参照）を `NoteEditorFragment` からすべて削除する
- [ ] `ActivityResultContracts` ランチャー群をラムダとして `NoteEditorScreen` に渡す
- [ ] `PollEditorFragment` のトランザクションを削除し `PollEditorLayout` を直接呼ぶ
- [ ] 既存の `ComposeView` 島（toolbar・filePreview・userActionMenu）を `NoteEditorScreen` 内に統合する
- [ ] `UserChipListAdapter` + RecyclerView を `NoteEditorAddressSection` に置換する
- [ ] `fragment_note_editor.xml` を削除する
- [ ] `PollEditorFragment.kt` を削除する
- [ ] `./gradlew :app:assembleDebug` BUILD SUCCESSFUL を確認
- [ ] 実機またはエミュレータで以下の動作確認:
  - [ ] テキスト入力・絵文字補完が動作する
  - [ ] CW フィールドの表示/非表示が動作する
  - [ ] ファイル添付が動作する
  - [ ] 投票エディタが動作する
  - [ ] 宛先ユーザー選択が動作する
  - [ ] 予約投稿の設定が動作する
  - [ ] バックプレスで下書き保存確認が出る
  - [ ] 投稿が正常に完了する

---

## Phase 4: Activity を setContent に移行

**ゴール:** `NoteEditorFragment` を削除し、Activity が直接 `setContent` する形に変える。

**前提条件:** Phase 3 完了・動作確認済み

### NoteEditorActivity の変更

```kotlin
@AndroidEntryPoint
class NoteEditorActivity : AppCompatActivity() {
    // AppCompatActivity のまま継続（ComponentActivity にすると AppCompat 依存が壊れる可能性あり）

    private val viewModel: NoteEditorViewModel by viewModels()

    // ActivityResultContracts ランチャーを Fragment から Activity に移動
    private val openDriveLauncher = registerForActivityResult(...) { ... }
    private val pickMultipleMediaLauncher = registerForActivityResult(...) { ... }
    // ...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyTheme()

        if (savedInstanceState == null) {
            viewModel.initialize(parseNoteEditorArgs(intent))
        }

        setContent {
            MilkteaStyleConfigApplyAndTheme(...) {
                NoteEditorScreen(
                    viewModel = viewModel,
                    onNavigateUp = { finish() },
                    onPickFileFromDrive = { openDriveLauncher.launch(...) },
                    ...
                )
            }
        }
    }
}
```

**注意:** `ComponentActivity` への変更は避ける。`AppCompatActivity` は `ComponentActivity` を継承しており `setContent { }` も使えるため、互換性を保つために `AppCompatActivity` のままにする。

### Intent API の維持

`newBundle()` コンパニオン関数は呼び出し元が多数あるため変更不要。Activity の内部実装だけ変わる。

### Window Insets の移行

Fragment が消えた後は `NoteEditorScreen` の Scaffold/Column 側で `WindowInsets` を処理する：

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    ...
)
```

### バックプレス処理の移行

```kotlin
// NoteEditorScreen 内
BackHandler(enabled = uiState.hasContent) {
    showSaveAsDraftDialog = true
}
```

### Intent 引数パース

`parseNoteEditorArgs(intent: Intent): NoteEditorArgs` として独立関数に切り出す。

### `isSaveNoteAsDraft` フローの処理

現在 `Handler(Looper.getMainLooper()).post { finish() }` で処理している部分を `LaunchedEffect` に置き換える：

```kotlin
LaunchedEffect(Unit) {
    viewModel.isSaveNoteAsDraft.collect { draftId ->
        if (draftId != null) onNavigateUp()
    }
}
```

### チェックリスト

- [ ] `ActivityResultContracts` ランチャー群を `NoteEditorFragment` から `NoteEditorActivity` に移動する
- [ ] `NoteEditorActivity.onCreate` を `setContent { NoteEditorScreen(...) }` に変更する
- [ ] `parseNoteEditorArgs(intent)` 関数を切り出す
- [ ] `NoteEditorScreen` 内に `BackHandler` を追加する
- [ ] `NoteEditorScreen` の Scaffold に `contentWindowInsets = WindowInsets.safeDrawing` を設定する
- [ ] `LaunchedEffect` で `isSaveNoteAsDraft` / `isPost` フローを監視する実装に変更する
- [ ] `NoteEditorFragment.kt` を削除する
- [ ] `activity_note_editor.xml` を削除する
- [ ] `./gradlew :app:assembleDebug` BUILD SUCCESSFUL を確認
- [ ] 実機またはエミュレータで Phase 3 と同じ動作確認項目を再確認する

---

## Phase 5: ダイアログの純粋 Compose 化（任意）

**ゴール:** Fragment ベースのダイアログを純粋 Compose の `Dialog { }` / `AlertDialog` composable に置き換える。

**前提条件:** Phase 4 完了。優先度低・任意対応。

ほとんどのダイアログは既に Compose inside Fragment になっている。以下を完全 Compose 化する：

| 現在 | 移行後 |
|------|--------|
| `ConfirmSaveAsDraftDialog` (Fragment) | `AlertDialog` composable（`showSaveAsDraftDialog: Boolean` state で制御） |
| `NoteEditorFileSizeWarningDialog` | 同上 |
| `ReservationPostDatePickerDialog` | `DatePickerDialog` composable |
| `ReservationPostTimePickerDialog` | `TimePickerDialog` composable |
| `PollDatePickerDialog` | 同上 |
| `PollTimePickerDialog` | 同上 |

`VisibilitySelectionDialogV2`・`NoteEditorSwitchAccountDialog`・`EditFileCaptionDialog`・`EditFileNameDialog` は既に Compose ベースなので、Fragment ラッパーを外して `Dialog { }` composable に変えるだけでよい。

### チェックリスト

- [ ] `ConfirmSaveAsDraftDialog` を `NoteEditorScreen` 内の `AlertDialog` composable に置き換える
- [ ] `NoteEditorFileSizeWarningDialog` を `AlertDialog` composable に置き換える
- [ ] `ReservationPostDatePickerDialog` / `ReservationPostTimePickerDialog` を `DatePickerDialog` / `TimePickerDialog` composable に置き換える
- [ ] `PollDatePickerDialog` / `PollTimePickerDialog` を composable に置き換える
- [ ] `VisibilitySelectionDialogV2` の Fragment ラッパーを外す
- [ ] `NoteEditorSwitchAccountDialog` の Fragment ラッパーを外す
- [ ] `EditFileCaptionDialog` / `EditFileNameDialog` の Fragment ラッパーを外す
- [ ] `./gradlew :app:assembleDebug` BUILD SUCCESSFUL を確認

---

## 技術的難所まとめ

### 1. MultiAutoCompleteTextView（Phase 2 で対処）

Compose TextField には絵文字補完機能がないため `AndroidView` でブリッジ。
将来的には `BasicTextField` + カスタムドロップダウンに置き換え可能だが初期移行では不要。

### 2. ActivityResultContracts の配置（Phase 3→4 で対処）

`rememberLauncherForActivityResult` は Composable 内でも使えるが、Activity/Fragment レベルで登録した方が
ライフサイクルの扱いが安全。今回は最終的に Activity に集約する。

### 3. テーマ適用（Phase 4 で確認必要）

`applyTheme()` が `AppCompatDelegate` に依存しているため、`AppCompatActivity` を維持することで解決する。
`ComponentActivity` への変更は今回の範囲外とする。

### 4. `isSaveNoteAsDraft` フローの処理（Phase 4 で対処）

現在 `Handler(Looper.getMainLooper()).post { finish() }` で処理している。
Compose 移行後は `LaunchedEffect` で監視する（詳細は Phase 4 参照）。

---

## SimpleEditorFragment について

`SimpleEditorFragment` は `NoteEditorActivity` とは別のエントリーポイント（メインフィード画面に埋め込み）。
`goToNormalEditor()` から `NoteEditorActivity.newBundle()` を使って遷移するため、今回の移行の影響を受けない。
`SimpleEditorFragment` 自体の Compose 化は別タスクとして切り出す。
Phase 2 で作成した `EmojiAutoCompleteTextField` を再利用できる。

---

## 参考ファイル

| ファイル | パス |
|---------|------|
| NoteEditorActivity | `modules/features/note/src/main/java/net/pantasystem/milktea/note/NoteEditorActivity.kt` |
| NoteEditorFragment | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/NoteEditorFragment.kt` |
| SimpleEditorFragment | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/SimpleEditorFragment.kt` |
| NoteEditorViewModel | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/viewmodel/NoteEditorViewModel.kt` |
| NoteEditorUiState | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/viewmodel/NoteEditorUiState.kt` |
| fragment_note_editor.xml | `modules/features/note/src/main/res/layout/fragment_note_editor.xml` |
| item_note_editor_reply_to_note.xml | `modules/features/note/src/main/res/layout/item_note_editor_reply_to_note.xml` |
| NoteEditorToolbar | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/NoteEditorToolbar.kt` |
| NoteEditorUserActionMenuLayout | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/NoteEditorUserActionMenuLayout.kt` |
| PollEditorLayout | `modules/features/note/src/main/java/net/pantasystem/milktea/note/editor/poll/PollEditorLayout.kt` |
