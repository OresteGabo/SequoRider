package dev.orestegabo.sequorider.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class SequoContrast {
    Normal,
    Medium,
    High,
}

@Composable
fun SequoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    contrast: SequoContrast = SequoContrast.Normal,
    content: @Composable () -> Unit,
) {
    val colorScheme = sequoColorScheme(
        darkTheme = darkTheme,
        contrast = contrast,
    )
    val semanticColors = if (darkTheme) {
        sequoSemanticColorsDark
    } else {
        sequoSemanticColorsLight
    }

    CompositionLocalProvider(LocalSequoSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = sequoTypography,
            shapes = sequoShapes,
            content = content,
        )
    }
}
