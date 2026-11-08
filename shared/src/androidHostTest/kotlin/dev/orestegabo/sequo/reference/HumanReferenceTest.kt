package dev.orestegabo.sequo.reference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Fixed v1 cross-platform vectors from contracts/human-reference-v1.json; no identifiers or credentials. */
class HumanReferenceTest {
    @Test fun canonicalVectorsMatchTheVersionOneWireContract() {
        val vectors = listOf(
            Triple(PublicReferenceKind.CHECKOUT, "000000", "H00-000-0TP"),
            Triple(PublicReferenceKind.ORDER, "123456", "C12-345-6E6"),
            Triple(PublicReferenceKind.MERCHANT_ORDER, "ABCDEF", "MAB-CDE-FJZ"),
            Triple(PublicReferenceKind.DELIVERY, "Z9X8W7", "DZ9-X8W-7P3"),
            Triple(PublicReferenceKind.PARCEL, "0011ZZ", "P00-11Z-Z7J"),
            Triple(PublicReferenceKind.RETURN, "987654", "R98-765-4Q4"),
            Triple(PublicReferenceKind.GROUP, "000000", "G00-000-0BH"),
            Triple(PublicReferenceKind.USER, "123456", "A12-345-6BJ"),
            Triple(PublicReferenceKind.MERCHANT, "ABCDEF", "VAB-CDE-FVD"),
            Triple(PublicReferenceKind.COURIER, "Z9X8W7", "TZ9-X8W-791"),
            Triple(PublicReferenceKind.RELAY, "0011ZZ", "S00-11Z-ZE0"),
            Triple(PublicReferenceKind.PRODUCT, "987654", "B98-765-4YF"),
            Triple(PublicReferenceKind.FINANCIAL, "000000", "F00-000-09J"),
            Triple(PublicReferenceKind.INSTRUCTION, "123456", "N12-345-69H"),
            Triple(PublicReferenceKind.LOCKER, "ABCDEF", "KAB-CDE-F6C"),
            Triple(PublicReferenceKind.BARGAINING, "Z9X8W7", "EZ9-X8W-7MS"),
            Triple(PublicReferenceKind.PACKAGE, "0011ZZ", "J00-11Z-ZHV"),
            Triple(PublicReferenceKind.LEDGER, "123456", "W12-345-65Q"),
            Triple(PublicReferenceKind.COOPERATIVE, "123456", "Q12-345-6TC"),
        )
        vectors.forEach { (kind, payload, reference) ->
            assertEquals(reference, HumanReference.encode(kind, payload))
            assertEquals(reference, HumanReference.normalize(reference))
            assertTrue(HumanReference.isValid(reference, kind))
            assertEquals(kind, HumanReference.kind(reference))
        }
    }

    @Test fun supportedTranscriptionAliasesNormalizeWithoutInventingAnotherReference() {
        assertEquals("P00-11Z-Z7J", HumanReference.normalize(" pOo ilz z7j "))
        assertEquals("R98-765-4Q4", HumanReference.normalize("r987654q4"))
    }

    @Test fun wrongChecksumTypeAndNonAsciiInputAreRejectedOffline() {
        assertFalse(HumanReference.isValid("R98-765-4Q5", PublicReferenceKind.RETURN))
        assertFalse(HumanReference.isValid("C12-345-6E6", PublicReferenceKind.RETURN))
        listOf("R98-765-4Q", "R98-765-4Q44", "R98\t7654Q4", "R98-765-4QＱ", "R98/765/4Q4", "x".repeat(65)).forEach {
            assertNull(HumanReference.normalize(it))
        }
    }
}
