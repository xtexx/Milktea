# Milktea Android SDK 対応チェックリスト

このファイルはAndroid最新SDK対応の進捗管理用です。
作業完了したタスクは `- [ ]` を `- [x]` に変えてください。

---

## Phase 1: Material3 テーマ移行

Compose 画面の Edge-to-edge 対応の前提条件。

- [ ] `app/src/main/res/values-v23/themes.xml` の `Theme.MaterialComponents` → `Theme.Material3` に変更
- [ ] `app/src/main/res/values-v27/themes.xml` の `Theme.MaterialComponents` → `Theme.Material3` に変更
- [ ] `modules/common_resource/src/main/res/values/themes.xml` の `Theme.MaterialComponents` → `Theme.Material3` に変更
- [ ] `app/src/main/res/values-v21/styles.xml` の古いスタイル整理
- [ ] Material3 移行後のビルドエラー・クラッシュ確認

---

## Phase 2: enableEdgeToEdge() 追加 + テーマのシステムバー色設定を削除

`enableEdgeToEdge()` がシステムバー色を管理するため、テーマ側の設定と競合する。

- [ ] テーマから `android:navigationBarColor` を削除（values-v23, v27, common_resource）
- [ ] テーマから `android:statusBarColor` を削除
- [ ] テーマから `android:windowLightNavigationBar` を削除（enableEdgeToEdge が自動制御）
- [ ] 全 Activity に `enableEdgeToEdge()` を追加（onCreate の setContentView より前）

---

## Phase 3: 簡単な Activity の個別 Insets 対応

`adjustResize` → `adjustNothing` への変更 + `ViewCompat.setOnApplyWindowInsetsListener` で Insets を手動適用。

- [ ] `AuthorizationActivity` — `windowSoftInputMode` 変更 + Insets 対応
- [ ] `SearchActivity` — `windowSoftInputMode` 変更 + Insets 対応
- [ ] `GalleryPostsActivity` — `windowSoftInputMode` 変更 + Insets 対応
- [ ] `SearchAndSelectUserActivity` — `windowSoftInputMode` 変更 + Insets 対応

---

## Phase 4: MainActivity（DrawerLayout）対応

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

DrawerLayout は `fitsSystemWindows` の挙動が変わるため個別対応が必要。

- [ ] `activity_main.xml` の DrawerLayout から `android:fitsSystemWindows="true"` を削除
- [ ] `activity_main.xml` の NavigationView から `android:fitsSystemWindows="true"` を削除
- [ ] `MainActivity` に `enableEdgeToEdge()` 追加
- [ ] `MainActivity` のルートビューに `WindowInsetsCompat` で top/bottom padding を適用
- [ ] ナビゲーションドロワーの表示確認（システムバーに重ならないか）
- [ ] `windowSoftInputMode="adjustPan"` → `adjustNothing` への変更検討

---

## Phase 5: キーボード絡みの Activity 対応

IME（ソフトキーボード）表示時のレイアウト調整が必要な Activity。

- [ ] `NoteEditorActivity` — `adjustResize` → `adjustNothing` + `WindowInsetsCompat.Type.ime()` で対応
- [ ] `MessageActivity` — `adjustResize` → `adjustNothing` + IME Insets でチャット UI を押し上げ

---

## Phase 6: Compose 画面の Insets 対応

- [ ] `AuthScreen.kt` — `Scaffold` の `contentWindowInsets = WindowInsets.safeDrawing` に変更
- [ ] `MessageScreen.kt` — `Scaffold` の `contentWindowInsets = WindowInsets.safeDrawing` に変更
- [ ] その他 Scaffold 使用画面の確認・統一
- [ ] `windowInsetsPadding` で `statusBars` が漏れている箇所の修正

---

## Phase 7: ライブラリの非推奨対応

### Accompanist 廃止ライブラリの置き換え
- [ ] `accompanist-swiperefresh`（0.25.1）→ Material3 の `PullToRefreshBox` に置き換え
- [ ] `accompanist-pager`（0.14.0）→ `HorizontalPager` / `VerticalPager`（Compose Foundation）に置き換え

---

## Phase 8: SDK・ライブラリバージョン更新

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
- [ ] `swiperefreshlayout` のアルファ版（`1.2.0-alpha01`）→ 安定版 `1.1.0` またはComposeに移行

### benchmark モジュール
- [ ] `benchmark/build.gradle` の Java バージョンを `1.8` → `17` に統一

---

## 参考：主要ファイルパス

| 内容 | パス |
|------|------|
| アプリ build.gradle | `app/build.gradle` |
| バージョンカタログ | `libs.versions.toml` |
| AndroidManifest | `app/src/main/AndroidManifest.xml` |
| MainActivity | `app/src/main/java/jp/panta/misskeyandroidclient/MainActivity.kt` |
| メインレイアウト | `app/src/main/res/layout/activity_main.xml` |
| テーマ（v23） | `app/src/main/res/values-v23/themes.xml` |
| テーマ（v27） | `app/src/main/res/values-v27/themes.xml` |
| 共通テーマ | `modules/common_resource/src/main/res/values/themes.xml` |
| AuthScreen | `modules/features/auth/src/main/java/net/pantasystem/milktea/auth/AuthScreen.kt` |
| MessageScreen | `modules/features/messaging/src/main/java/net/pantasystem/milktea/messaging/MessageScreen.kt` |
