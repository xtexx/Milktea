package net.pantasystem.milktea.common_compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import net.pantasystem.milktea.model.setting.AvatarIconShapeType
import net.pantasystem.milktea.model.setting.DefaultConfig
import net.pantasystem.milktea.model.setting.LocalConfigRepository
import net.pantasystem.milktea.model.setting.Theme

// White テーマ（ライト、インディゴ）
private val MilkteaWhiteColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF575DAE),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
)

// Dark テーマ（ダーク、インディゴ）
private val MilkteaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF6D7FE1),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF0E0E0E),
)

// Black テーマ（ダーク、真黒）
private val MilkteaBlackColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF6D7FE1),
    surface = Color(0xFF000000),
    background = Color(0xFF000000),
)

// Bread テーマ（ライト、パン色）
private val MilkteaBreadColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFD9A042),
    secondary = Color(0xFF8BC34A),
    surface = Color(0xFFF8F5C9),
    background = Color(0xFFF8F5C9),
)

// ElephantDark テーマ（ダーク、マストドン紫）
private val MilkteaElephantDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8C8DFE),
    secondary = Color(0xFF595AFF),
    surface = Color(0xFF282C37),
    background = Color(0xFF191B22),
)

/**
 * ユーザーが選択したテーマに対応する M3 ColorScheme を返す。
 * ThemeUtil.kt で Activity に適用される XML テーマと同じ Theme enum で同期される。
 */
fun Theme.toColorScheme(): ColorScheme = when (this) {
    Theme.White -> MilkteaWhiteColorScheme
    Theme.Dark -> MilkteaDarkColorScheme
    Theme.Black -> MilkteaBlackColorScheme
    Theme.Bread -> MilkteaBreadColorScheme
    Theme.ElephantDark -> MilkteaElephantDarkColorScheme
}

@Composable
fun MilkteaStyleConfigApplyAndTheme(
    configRepository: LocalConfigRepository,
    content: @Composable () -> Unit,
) {
    val config by configRepository.observe().collectAsState(initial = DefaultConfig.config)
    MaterialTheme(colorScheme = config.theme.toColorScheme()) {
        CompositionLocalProvider(
            LocalAvatarIconShape provides when (config.avatarIconShapeType) {
                AvatarIconShapeType.Circle -> Shape.Circle
                AvatarIconShapeType.Square -> Shape.RoundedCorner
            },
        ) {
            content()
        }
    }
}
