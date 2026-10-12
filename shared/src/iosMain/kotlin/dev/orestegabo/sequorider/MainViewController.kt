package dev.orestegabo.sequorider

import androidx.compose.ui.window.ComposeUIViewController
import dev.orestegabo.sequorider.core.auth.GoogleSignInFailureReason
import dev.orestegabo.sequorider.core.auth.GoogleSignInResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun MainViewController(
    onGoogleSignIn: (((GoogleSignInResult) -> Unit) -> Unit) = { complete ->
        complete(GoogleSignInResult.Failure(GoogleSignInFailureReason.NotConfigured))
    },
    onGoogleSignOut: () -> Unit = {},
) = ComposeUIViewController {
    App(
        onGoogleSignIn = {
            suspendCancellableCoroutine { continuation ->
                onGoogleSignIn { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
            }
        },
        onGoogleSignOut = { onGoogleSignOut() },
    )
}
