package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.core.auth.AuthKeychainStore
import dev.orestegabo.sequorider.core.auth.KeychainStorageException
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthKeychainRoundTripTest {
    // Requires a legitimately signed iOS app/test host. Unhosted simctl binaries return errSecMissingEntitlement.
    @Ignore
    @Test fun nativeDictionaryAndRetainedDataBridgeRoundTripWithoutTouchingSessionRecords() = runBlocking {
        val unique = NSUUID().UUIDString
        val keychain = AuthKeychainStore("dev.orestegabo.sequorider.keychain-roundtrip.$unique")
        val account = "synthetic-test-$unique"
        var failure: Throwable? = null
        try {
            assertNull(keychain.get(account))
            keychain.save(account, "Synthetic local value — éè 👋")
            assertEquals("Synthetic local value — éè 👋", keychain.get(account))
            keychain.save(account, "Updated synthetic value — àç")
            assertEquals("Updated synthetic value — àç", keychain.get(account))
            keychain.clear(account)
            assertNull(keychain.get(account))
        } catch (error: Throwable) {
            failure = error
            if (error is KeychainStorageException) {
                throw AssertionError("Native Keychain ${error.operation} OSStatus=${error.status}", error)
            }
            throw error
        } finally {
            try { keychain.clear(account) }
            catch (cleanup: Throwable) { if (failure == null) throw cleanup }
        }
    }
}
