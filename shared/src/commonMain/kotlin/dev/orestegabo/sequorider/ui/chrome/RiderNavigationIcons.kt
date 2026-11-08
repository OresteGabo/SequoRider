/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.orestegabo.sequorider.ui.chrome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.model.RiderSection

// Existing Material icon paths, kept local to avoid adding a dependency for five navigation icons.
internal fun RiderSection.navigationIcon(): ImageVector = when (this) {
    RiderSection.Missions -> missionIcon
    RiderSection.ActiveRoute -> routeIcon
    RiderSection.History -> historyIcon
    RiderSection.Payout -> paymentsIcon
    RiderSection.Account -> personIcon
}

private val missionIcon: ImageVector by lazy {
    ImageVector.Builder("Assignment", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.0f, 3.0f)
            horizontalLineToRelative(-4.18f)
            curveTo(14.4f, 1.84f, 13.3f, 1.0f, 12.0f, 1.0f)
            curveToRelative(-1.3f, 0.0f, -2.4f, 0.84f, -2.82f, 2.0f)
            lineTo(5.0f, 3.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 5.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(12.0f, 3.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
            reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
            reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
            reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
            close()
            moveTo(14.0f, 17.0f)
            lineTo(7.0f, 17.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(7.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(17.0f, 13.0f)
            lineTo(7.0f, 13.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(17.0f, 9.0f)
            lineTo(7.0f, 9.0f)
            lineTo(7.0f, 7.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }.build()
}

private val routeIcon: ImageVector by lazy {
    ImageVector.Builder("Route", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.0f, 15.18f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
            reflectiveCurveToRelative(-4.0f, 1.79f, -4.0f, 4.0f)
            verticalLineToRelative(10.0f)
            curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
            reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
            verticalLineTo(8.82f)
            curveTo(8.16f, 8.4f, 9.0f, 7.3f, 9.0f, 6.0f)
            curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
            reflectiveCurveTo(3.0f, 4.34f, 3.0f, 6.0f)
            curveToRelative(0.0f, 1.3f, 0.84f, 2.4f, 2.0f, 2.82f)
            verticalLineTo(17.0f)
            curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
            reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
            verticalLineToRelative(8.18f)
            curveToRelative(-1.16f, 0.41f, -2.0f, 1.51f, -2.0f, 2.82f)
            curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
            reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
            curveTo(21.0f, 16.7f, 20.16f, 15.6f, 19.0f, 15.18f)
            close()
        }
    }.build()
}

private val historyIcon: ImageVector by lazy {
    ImageVector.Builder("History", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(13.0f, 3.0f)
            curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
            lineTo(1.0f, 12.0f)
            lineToRelative(3.89f, 3.89f)
            lineToRelative(0.07f, 0.14f)
            lineTo(9.0f, 12.0f)
            lineTo(6.0f, 12.0f)
            curveToRelative(0.0f, -3.87f, 3.13f, -7.0f, 7.0f, -7.0f)
            reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
            reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
            curveToRelative(-1.93f, 0.0f, -3.68f, -0.79f, -4.94f, -2.06f)
            lineToRelative(-1.42f, 1.42f)
            curveTo(8.27f, 19.99f, 10.51f, 21.0f, 13.0f, 21.0f)
            curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
            reflectiveCurveToRelative(-4.03f, -9.0f, -9.0f, -9.0f)
            close()
            moveTo(12.0f, 8.0f)
            verticalLineToRelative(5.0f)
            lineToRelative(4.28f, 2.54f)
            lineToRelative(0.72f, -1.21f)
            lineToRelative(-3.5f, -2.08f)
            lineTo(13.5f, 8.0f)
            lineTo(12.0f, 8.0f)
            close()
        }
    }.build()
}

private val paymentsIcon: ImageVector by lazy {
    ImageVector.Builder("Payments", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.0f, 14.0f)
            lineTo(19.0f, 6.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            lineTo(3.0f, 4.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(8.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            close()
            moveTo(10.0f, 13.0f)
            curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
            reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
            reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
            reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
            close()
            moveTo(23.0f, 7.0f)
            verticalLineToRelative(11.0f)
            curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
            lineTo(4.0f, 20.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(17.0f)
            lineTo(21.0f, 7.0f)
            horizontalLineToRelative(2.0f)
            close()
        }
    }.build()
}

private val personIcon: ImageVector by lazy {
    ImageVector.Builder("Person", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 12.0f)
            curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
            reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
            reflectiveCurveToRelative(-4.0f, 1.79f, -4.0f, 4.0f)
            reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
            close()
            moveTo(12.0f, 14.0f)
            curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(16.0f)
            verticalLineToRelative(-2.0f)
            curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
            close()
        }
    }.build()
}
