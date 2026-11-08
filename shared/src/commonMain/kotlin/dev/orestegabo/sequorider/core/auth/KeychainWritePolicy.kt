package dev.orestegabo.sequorider.core.auth

internal enum class KeychainWriteResult { Success, NotFound, Duplicate, Failed }

/** Existing credentials are never deleted to make a replacement write. */
internal fun writeKeychainValue(update: () -> KeychainWriteResult, add: () -> KeychainWriteResult) {
    val result = when (val updated = update()) {
        KeychainWriteResult.NotFound -> when (val added = add()) {
            KeychainWriteResult.Duplicate -> update()
            else -> added
        }
        else -> updated
    }
    check(result == KeychainWriteResult.Success) { "Secure session storage is unavailable. Please try again." }
}
