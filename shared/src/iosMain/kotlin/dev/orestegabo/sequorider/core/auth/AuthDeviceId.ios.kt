package dev.orestegabo.sequorider.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDevice

@Composable
actual fun rememberAuthDeviceId(): String =
    remember {
        UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "ios-rider-device"
    }
