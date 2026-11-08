package dev.orestegabo.sequorider.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.orestegabo.sequo.design.SequoLightColors
import dev.orestegabo.sequo.design.SequoDarkColors
import dev.orestegabo.sequo.design.SequoTypography
import dev.orestegabo.sequo.design.SequoThemeShapes

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
    val colorScheme = if (contrast == SequoContrast.Normal) {
        if (darkTheme) SequoDarkColors else SequoLightColors
    } else {
        // Retain Rider's supported accessibility palettes for callers requesting extra contrast.
        sequoColorScheme(darkTheme = darkTheme, contrast = contrast)
    }
    val semanticColors = if (darkTheme) {
        sequoSemanticColorsDark
    } else {
        sequoSemanticColorsLight
    }

    CompositionLocalProvider(LocalSequoSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SequoTypography,
            shapes = SequoThemeShapes,
            content = content,
        )
    }
}
