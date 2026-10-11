package dev.orestegabo.sequorider

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.orestegabo.sequorider.theme.SequoTheme
import dev.orestegabo.sequorider.ui.app.SequoRiderShell

@Composable
@Preview
fun App() {
    SequoTheme {
        SequoRiderShell()
    }
}
