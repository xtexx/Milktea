# Milktea Android SDK 対応チェックリスト

このファイルはAndroid最新SDK対応の進捗管理用です。
作業完了したタスクは `- [ ]` を `- [x]` に変えてください。

---

## Phase 1: M3 テーマ基盤の構築

**全 Compose 作業の前提条件。最初に完了させる。**

### 1-1. XML テーマを Material3 に移行
- [x] `app/src/main/res/values-v23/themes.xml` — `Theme.MaterialComponents` → `Theme.Material3`、システムバー色属性を削除
- [x] `app/src/main/res/values-v27/themes.xml` — 同上
- [x] `modules/common_resource/src/main/res/values/themes.xml` — 同上（Dark/Black/Bread/ElephantDark テーマ）
- [x] `app/src/main/res/values-v21/styles.xml` — 古いスタイル整理
- [x] `modules/common_resource/src/main/res/values/styles.xml` — Widget.MaterialComponents.* → Widget.Material3.* に更新

### 1-2. Compose M3 テーマラッパーの作成
現在 `MdcTheme`（Material2 ブリッジ）を使用中 → M3 の `MaterialTheme` に置き換える。
- [x] `modules/common_compose/MilkteaTheme.kt` に M3 用 `ColorScheme` を 5テーマ分定義
  - White / Dark / Black / Bread / ElephantDark
  - `Theme.toColorScheme()` 拡張関数で ThemeUtil.kt の Theme enum と同期
- [x] `MdcTheme { }` → `MaterialTheme(colorScheme = ...) { }` に置き換え
- [x] `libs.versions.toml` に `compose-material3 = "1.3.1"` を追加
- [x] `common_compose/build.gradle` で material2 → material3 に差し替え、MdcTheme アダプター削除
- [x] ビルド確認（Phase 2 の import 置き換え前なのでコンパイルエラーが大量に出る想定）

---

## Phase 2: Compose 全体の M3 移行

**ビルドエラーを潰しながら進める。一気にやらず機能モジュール単位で対応。**

### 2-1. 全ファイルの import 置き換え
- [x] `androidx.compose.material.` → `androidx.compose.material3.` に一括置換（注意: API が変わるものがある）

### 2-2. API 変更対応（M2 → M3 で変わる主要コンポーネント）

| M2 | M3 | 対応ファイル数 |
|----|-----|------------|
| `MaterialTheme.colors.*` | `MaterialTheme.colorScheme.*` | 多数 |
| `TopAppBar(backgroundColor=)` | `TopAppBar(colors=TopAppBarDefaults.*)` | 多数 |
| `ModalBottomSheetLayout` | `ModalBottomSheet` | 3ファイル |
| `Scaffold(backgroundColor=)` | `Scaffold(containerColor=)` | 33ファイル |
| `Card(backgroundColor=)` | `Card(colors=CardDefaults.*)` | 多数 |
| `Divider` | `HorizontalDivider` | 複数 |
| `TabRow` | `TabRow`（ほぼ同じだが色指定が変わる） | 複数 |

### 2-3. `ModalBottomSheetLayout` → `ModalBottomSheet` の書き換え（API が別物）
- [x] `SearchAndSelectUserScreen.kt`
- [x] `TabItemsListScreen.kt`
- [x] `AccountSettingScreen.kt`

### 2-4. モジュール別動作確認
- [x] `features/auth` — AuthScreen, SignUpScreen 等
- [x] `features/setting` — 22ファイル（最多）
- [x] `features/note` — エディタ周り
- [x] `features/channel` — ChannelScreen
- [x] `features/drive` — DriveScreen
- [x] `features/user` — ユーザー画面
- [x] `features/messaging` — MessageScreen
- [x] `features/gallery` — GalleryEditorPage
- [x] `features/clip`, `group`, `userlist`, `search`, `account`
- [x] `common_compose`, `common_android_ui` — 共通コンポーネント

### ビルド確認
- [x] `./gradlew :app:assembleDebug` BUILD SUCCESSFUL

---

## Phase 3: Accompanist 廃止ライブラリの置き換え

M3 移行後に対応（M3 の PullToRefreshBox を使うため）。

- [ ] `accompanist-swiperefresh`（0.25.1）→ Material3 `PullToRefreshBox` に置き換え（`user/followrequests/FollowRequestsScreen.kt` 等）
- [ ] `accompanist-pager`（0.14.0）→ Compose Foundation の `HorizontalPager` / `VerticalPager` に置き換え
  - `ChannelScreen.kt`（`ExperimentalPagerApi` 使用中）
  - `DriveScreen.kt`（`ExperimentalPagerApi` 使用中）

---

## Phase 4: enableEdgeToEdge() 追加 + テーマのシステムバー色設定を削除

`enableEdgeToEdge()` がシステムバー色を管理するため、テーマ側の設定と競合する（Phase 1 で XML テーマから削除済みのはず）。

- [ ] 全 Activity に `enableEdgeToEdge()` を追加（`onCreate` の `setContentView` より前）

---

## Phase 5: Compose 画面の Insets 対応

Phase 4 で `enableEdgeToEdge()` が有効になった後、Compose 側の Insets を整備する。

- [ ] 全 `Scaffold` に `contentWindowInsets = WindowInsets.safeDrawing` を設定（33ファイル）
- [ ] `AuthScreen.kt` — 既存の `windowInsetsPadding` を `safeDrawing` に統一
- [ ] `MessageScreen.kt` — 同上

---

## Phase 6: 簡単な Android View Activity の個別 Insets 対応

`adjustResize` → `adjustNothing` への変更 + `ViewCompat.setOnApplyWindowInsetsListener` で Insets を手動適用。

- [ ] `AuthorizationActivity` — `windowSoftInputMode` 変更 + Insets 対応
- [ ] `SearchActivity` — `windowSoftInputMode` 変更 + Insets 対応
- [ ] `GalleryPostsActivity` — `windowSoftInputMode` 変更 + Insets 対応
- [ ] `SearchAndSelectUserActivity` — `windowSoftInputMode` 変更 + Insets 対応

---

## Phase 7: MainActivity（DrawerLayout）対応

DrawerLayout は `fitsSystemWindows` の挙動が変わるため個別対応が必要。

> **注意: ViewPager2 の Insets 非伝播問題**
> ViewPager2 は内部の `RecyclerView` が Window Insets を子 View に伝播しない既知の問題がある。
> 各 Fragment が独自に Insets を処理するか、以下のワークアラウンドが必要：
> ```kotlin
> ViewCompat.setOnApplyWindowInsetsListener(viewPager) { _, insets ->
>     for (i in 0 until viewPager.childCount) {
>         ViewCompat.dispatchApplyWindowInsets(viewPager.getChildAt(i), insets)
>     }
>     insets
> }
> ```
>
> **ViewPager2 使用箇所（要個別確認）:**
> - `TabFragment` — メインタブ（MainActivity 内）
> - `SearchTopFragment` — 検索タブ
> - `SearchResultActivity` — 検索結果タブ
> - `MediaActivity` — メディアビューア
> - `NoteDetailPagerFragment` — ノート詳細
> - `UserDetailActivity` — ユーザープロフィールタブ
> - `GalleryPostTabFragment` — ギャラリータブ
> - `NotificationMentionFragment` — 通知タブ
> - `EmojiPickerFragment` — 絵文字ピッカー
> - `ReactionHistoryPagerDialog` — リアクション履歴
> - `BottomSheetViewPager` — ボトムシート内 ViewPager

- [ ] `activity_main.xml` の DrawerLayout / NavigationView から `android:fitsSystemWindows="true"` を削除
- [ ] `MainActivity` に `enableEdgeToEdge()` 追加（Phase 4 で対応済みなら不要）
- [ ] `MainActivity` のルートビューに `WindowInsetsCompat` で top/bottom padding を適用
- [ ] ナビゲーションドロワーの表示確認
- [ ] `windowSoftInputMode="adjustPan"` → `adjustNothing` への変更検討
- [ ] `TabFragment` の ViewPager2 に Insets dispatch ワークアラウンドを追加

---

## Phase 8: キーボード絡みの Activity 対応

IME（ソフトキーボード）表示時のレイアウト調整が必要な Activity。

- [ ] `NoteEditorActivity` — `adjustResize` → `adjustNothing` + `WindowInsetsCompat.Type.ime()` で対応
- [ ] `MessageActivity` — `adjustResize` → `adjustNothing` + IME Insets でチャット UI を押し上げ

---

## Phase 9: SDK・ライブラリバージョン更新

### SDK
- [ ] `compileSdk` 34 → 35
- [ ] `targetSdk` 34 → 35

### AGP・ビルドツール
- [ ] AGP `8.1.3` → `8.7.x` 以上
- [ ] Google Services Plugin `4.3.15` → `4.4.x`

### ライブラリ
- [ ] Kotlin `2.0.0` → `2.1.x`
- [ ] Compose BOM 最新化（現在 `1.7.1`）
- [ ] Hilt `2.48.1` → `2.52.x`
- [ ] Room `2.6.0` → `2.7.x`
- [ ] Coil `2.4.0` → `3.x`（API 変更あり、要注意）
- [ ] OkHttp `4.10.0` → `4.12.x`
- [ ] Retrofit `2.9.0` → `2.11.x`
- [ ] Firebase BOM `32.2.2` → `33.x`
- [ ] kotlinx.datetime `0.4.0` → `0.6.x`
- [ ] kotlinx.serialization `1.6.3` → `1.7.x`
- [ ] `swiperefreshlayout` アルファ版（`1.2.0-alpha01`）→ 安定版 `1.1.0` または Compose に移行

### benchmark モジュール
- [ ] `benchmark/build.gradle` の Java バージョンを `1.8` → `17` に統一

---

## 参考：主要ファイルパス

| 内容 | パス |
|------|------|
| アプリ build.gradle | `app/build.gradle` |
| バージョンカタログ | `libs.versions.toml` |
| AndroidManifest | `app/src/main/AndroidManifest.xml` |
| Compose テーマ | `modules/common_compose/src/main/java/net/pantasystem/milktea/common_compose/MilkteaTheme.kt` |
| テーマユーティリティ | `app/src/main/java/jp/panta/misskeyandroidclient/ThemeUtil.kt` |
| MainActivity | `app/src/main/java/jp/panta/misskeyandroidclient/MainActivity.kt` |
| メインレイアウト | `app/src/main/res/layout/activity_main.xml` |
| テーマ（v23） | `app/src/main/res/values-v23/themes.xml` |
| テーマ（v27） | `app/src/main/res/values-v27/themes.xml` |
| 共通テーマ | `modules/common_resource/src/main/res/values/themes.xml` |
| AuthScreen | `modules/features/auth/src/main/java/net/pantasystem/milktea/auth/AuthScreen.kt` |
| MessageScreen | `modules/features/messaging/src/main/java/net/pantasystem/milktea/messaging/MessageScreen.kt` |
| ChannelScreen | `modules/features/channel/src/main/java/net/pantasystem/milktea/channel/ChannelScreen.kt` |
| DriveScreen | `modules/features/drive/src/main/java/net/pantasystem/milktea/drive/DriveScreen.kt` |
| 設定 Compose | `modules/features/setting/src/main/java/net/pantasystem/milktea/setting/` |
