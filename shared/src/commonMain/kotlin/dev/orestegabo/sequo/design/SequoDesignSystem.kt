package dev.orestegabo.sequo.design

// Shared SEQUO mobile design source. Keep all four copies byte-identical.
// Check or synchronize with sequo-api/scripts/mobile-design.py.
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SequoDesign {
    val ScreenGutter = 20.dp
    val SectionGap = 16.dp
    val CardPadding = 16.dp
    val ControlHeight = 48.dp
    val CardShape = RoundedCornerShape(20.dp)
    val ControlShape = RoundedCornerShape(18.dp)
    val NavContainer = RoundedCornerShape(28.dp)
    val NavItem = RoundedCornerShape(18.dp)
}

internal val SequoPrimary = Color(0xFF0B6B46)
internal val SequoSecondary = Color(0xFF087F4F)
internal val SequoAccent = Color(0xFF8A6200)
internal val SequoSurface = Color(0xFFFFFFFF)
internal val SequoBackground = Color(0xFFF7F8F4)
internal val SequoSurfaceVariant = Color(0xFFDDE6DF)
internal val SequoOnSurface = Color(0xFF0F1720)
internal val SequoOnSurfaceVariant = Color(0xFF3F4A44)
internal val SequoOutline = Color(0xFF6F7B73)

val SequoLightColors = lightColorScheme(
    primary = SequoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFD9),
    onPrimaryContainer = Color(0xFF002C1C),
    secondary = SequoSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC4F3D8),
    onSecondaryContainer = Color(0xFF003821),
    tertiary = SequoAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3A1),
    onTertiaryContainer = Color(0xFF2A1B00),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = SequoBackground,
    onBackground = SequoOnSurface,
    surface = SequoSurface,
    onSurface = SequoOnSurface,
    surfaceVariant = SequoSurfaceVariant,
    onSurfaceVariant = SequoOnSurfaceVariant,
    outline = SequoOutline,
    outlineVariant = Color(0xFFC5D0C8),
    scrim = Color.Black,
    inverseSurface = Color(0xFF25312B),
    inverseOnSurface = Color(0xFFF0F6EF),
    inversePrimary = Color(0xFF7AD8A9),
    surfaceDim = Color(0xFFD8DED8),
    surfaceBright = Color(0xFFFAFBF8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5F0),
    surfaceContainer = Color(0xFFEBF0EA),
    surfaceContainerHigh = Color(0xFFE5EBE4),
    surfaceContainerHighest = Color(0xFFDFE5DE),
)

val SequoDarkColors = darkColorScheme(
    primary = Color(0xFF7AD8A9),
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFFBFF4D7),
    secondary = Color(0xFF65D99D),
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF0B5F3A),
    onSecondaryContainer = Color(0xFFC5F4D8),
    tertiary = Color(0xFFF7C74E),
    onTertiary = Color(0xFF3D2D00),
    tertiaryContainer = Color(0xFF654700),
    onTertiaryContainer = Color(0xFFFFE4A6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1110),
    onBackground = Color(0xFFE4ECE6),
    surface = Color(0xFF101917),
    onSurface = Color(0xFFE4ECE6),
    surfaceVariant = Color(0xFF3E4A44),
    onSurfaceVariant = Color(0xFFC4D0C8),
    outline = Color(0xFF89978E),
    outlineVariant = Color(0xFF3D4A43),
    scrim = Color.Black,
    inverseSurface = Color(0xFFE4ECE6),
    inverseOnSurface = Color(0xFF101917),
    inversePrimary = SequoPrimary,
    surfaceDim = Color(0xFF0B1110),
    surfaceBright = Color(0xFF303B36),
    surfaceContainerLowest = Color(0xFF060B0A),
    surfaceContainerLow = Color(0xFF121D1A),
    surfaceContainer = Color(0xFF17231F),
    surfaceContainerHigh = Color(0xFF21302A),
    surfaceContainerHighest = Color(0xFF2B3A34),
)

val SequoTypography = Typography().let { baseline ->
    baseline.copy(
        headlineLarge = baseline.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        headlineMedium = baseline.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        headlineSmall = baseline.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleLarge = baseline.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleMedium = baseline.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleSmall = baseline.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        bodyLarge = baseline.bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = baseline.bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = baseline.bodySmall.copy(letterSpacing = 0.sp),
        labelLarge = baseline.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelMedium = baseline.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
        labelSmall = baseline.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    )
}

val SequoThemeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)


data class SequoNavDestination(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/** Insets belong to the app shell/native host, so this rail never doubles them. */
@Composable
fun SequoNavigationBar(
    destinations: List<SequoNavDestination>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = SequoDesign.NavContainer,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 3.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            destinations.forEach { destination ->
                val selected = destination.key == selectedKey
                val fill by animateColorAsState(if (selected) colors.primaryContainer else Color.Transparent)
                val ink by animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
                Surface(
                    modifier = Modifier.weight(1f).clip(SequoDesign.NavItem)
                        .selectable(selected = selected, enabled = enabled, role = Role.Tab,
                            onClick = { onSelect(destination.key) }),
                    shape = SequoDesign.NavItem,
                    color = fill,
                    contentColor = ink,
                ) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BadgedBox(badge = {
                            if (destination.badgeCount > 0) Badge {
                                Text(if (destination.badgeCount > 99) "99+" else destination.badgeCount.toString())
                            }
                        }) {
                            Icon(destination.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            destination.label,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 28.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SequoPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp)
                .padding(horizontal = SequoDesign.ScreenGutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            navigationIcon?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
fun SequoPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SequoDesign.CardShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        content = content,
    )
}

@Composable
fun SequoActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = SequoDesign.ControlHeight),
        enabled = enabled, shape = SequoDesign.ControlShape, colors = colors,
        contentPadding = contentPadding, content = content)
}

@Composable
fun SequoOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = SequoDesign.ControlHeight),
        enabled = enabled, shape = SequoDesign.ControlShape,
        contentPadding = contentPadding, content = content)
}
