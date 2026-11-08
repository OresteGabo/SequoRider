package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.core.auth.KeychainWriteResult.*
import dev.orestegabo.sequorider.core.auth.writeKeychainValue
import kotlin.test.*

class KeychainWritePolicyTest {
    @Test fun replacementUpdatesExistingCredentialWithoutDeleteOrAdd() {
        var adds = 0
        writeKeychainValue(update = { Success }, add = { adds++; Failed })
        assertEquals(0, adds)
    }
    @Test fun failedUpdatePreservesOldCredentialAndPropagatesFailure() {
        var stored = "existing refresh credential"; var adds = 0
        assertFailsWith<IllegalStateException> {
            writeKeychainValue(update = { Failed }, add = { adds++; stored = "replacement"; Success })
        }
        assertEquals("existing refresh credential", stored); assertEquals(0, adds)
    }
    @Test fun missingEntryAddsOnceAndConcurrentDuplicateRetriesUpdateWithoutDeletion() {
        var updates = 0; var adds = 0
        writeKeychainValue(update = { if (++updates == 1) NotFound else Success }, add = { adds++; Duplicate })
        assertEquals(2, updates); assertEquals(1, adds)
        assertFailsWith<IllegalStateException> { writeKeychainValue(update = { NotFound }, add = { Failed }) }
    }
}
