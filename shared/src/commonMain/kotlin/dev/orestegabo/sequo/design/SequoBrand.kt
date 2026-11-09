package dev.orestegabo.sequo.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import sequorider.shared.generated.resources.Res
import sequorider.shared.generated.resources.sequo_brand_symbol
import sequorider.shared.generated.resources.sequo_brand_symbol_dark
import sequorider.shared.generated.resources.sequo_brand_logo_light
import sequorider.shared.generated.resources.sequo_brand_logo_dark
import sequorider.shared.generated.resources.sequo_brand_wordmark_light
import sequorider.shared.generated.resources.sequo_brand_wordmark_dark
import sequorider.shared.generated.resources.sequo_brand_stacked_light
import sequorider.shared.generated.resources.sequo_brand_stacked_dark

// Logo artwork follows the app's selected theme, including explicit dark mode.
@Composable
private fun darkBrandSurface() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun sequoBrandSymbolPainter(): Painter = painterResource(
    if (darkBrandSurface()) Res.drawable.sequo_brand_symbol_dark else Res.drawable.sequo_brand_symbol,
)

@Composable
fun sequoBrandWordmarkPainter(): Painter = painterResource(
    if (darkBrandSurface()) Res.drawable.sequo_brand_wordmark_dark else Res.drawable.sequo_brand_wordmark_light,
)

@Composable
fun sequoBrandLogoPainter(stacked: Boolean = false): Painter = painterResource(
    if (stacked) {
        if (darkBrandSurface()) Res.drawable.sequo_brand_stacked_dark else Res.drawable.sequo_brand_stacked_light
    } else {
        if (darkBrandSurface()) Res.drawable.sequo_brand_logo_dark else Res.drawable.sequo_brand_logo_light
    },
)
