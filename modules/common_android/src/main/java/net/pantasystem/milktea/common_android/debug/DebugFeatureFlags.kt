package net.pantasystem.milktea.common_android.debug

import android.content.Context

/**
 * 開発時専用のフィーチャーフラグ管理。
 * SharedPreferences に保存し、アプリ再起動なしで切り替え可能。
 *
 * **呼び出し側を BuildConfig.DEBUG で囲って本番ビルドへの影響をゼロにすること。**
 */
object DebugFeatureFlags {

    private const val PREFS_NAME = "debug_feature_flags"
    private const val KEY_USE_COMPOSE_TIMELINE = "use_compose_timeline"

    /**
     * Compose 製タイムラインを使用するかどうか。
     * true : ComposeView + LazyColumn + NoteCard
     * false: 既存 RecyclerView + TimelineListAdapter（デフォルト）
     */
    fun isComposeTimelineEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_COMPOSE_TIMELINE, false)

    fun setComposeTimelineEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_COMPOSE_TIMELINE, enabled)
            .apply()
    }
}
