package dev.orestegabo.sequorider.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Immutable
data class SequoSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val deliveryActive: Color,
    val onDeliveryActive: Color,
    val pickupReady: Color,
    val onPickupReady: Color,
)

val sequoSemanticColorsLight = SequoSemanticColors(
    success = Color(0xFF168153),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD6F5E2),
    onSuccessContainer = Color(0xFF06351F),
    warning = Color(0xFF8A6400),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFE6A8),
    onWarningContainer = Color(0xFF271A00),
    info = Color(0xFF1C667D),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFC8EEFA),
    onInfoContainer = Color(0xFF001F29),
    deliveryActive = Color(0xFF0B6B46),
    onDeliveryActive = Color(0xFFFFFFFF),
    pickupReady = Color(0xFFB57412),
    onPickupReady = Color(0xFFFFFFFF),
)

val sequoSemanticColorsDark = SequoSemanticColors(
    success = Color(0xFF73D9A4),
    onSuccess = Color(0xFF00391F),
    successContainer = Color(0xFF075E37),
    onSuccessContainer = Color(0xFFD6F5E2),
    warning = Color(0xFFF4C650),
    onWarning = Color(0xFF412D00),
    warningContainer = Color(0xFF604600),
    onWarningContainer = Color(0xFFFFE6A8),
    info = Color(0xFF8DD3EA),
    onInfo = Color(0xFF003543),
    infoContainer = Color(0xFF004D61),
    onInfoContainer = Color(0xFFC8EEFA),
    deliveryActive = Color(0xFF63D99F),
    onDeliveryActive = Color(0xFF003823),
    pickupReady = Color(0xFFFFB96B),
    onPickupReady = Color(0xFF492900),
)

internal val LocalSequoSemanticColors = staticCompositionLocalOf { sequoSemanticColorsLight }

val MaterialTheme.sequoColors: SequoSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSequoSemanticColors.current
