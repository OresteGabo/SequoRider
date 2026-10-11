package dev.orestegabo.sequorider

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.orestegabo.sequorider.core.auth.GoogleSignInFailureReason
import dev.orestegabo.sequorider.core.auth.GoogleSignInResult
import dev.orestegabo.sequorider.theme.SequoTheme
import dev.orestegabo.sequorider.ui.app.SequoRiderShell

@Composable
@Preview
fun App(
    onGoogleSignIn: suspend () -> GoogleSignInResult = {
        GoogleSignInResult.Failure(GoogleSignInFailureReason.NotConfigured)
    },
    onGoogleSignOut: suspend () -> Unit = {},
) {
    SequoTheme {
        SequoRiderShell(
            onGoogleSignIn = onGoogleSignIn,
            onGoogleSignOut = onGoogleSignOut,
        )
    }
}
