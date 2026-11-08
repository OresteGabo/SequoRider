package dev.orestegabo.sequorider.core.auth

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.errSecDuplicateItem
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
internal class AuthKeychainStore(
    private val service: String = "dev.orestegabo.sequorider.auth",
) {
    suspend fun save(account: String, value: String) {
        val data = checkNotNull(value.toNSData()) { "Secure session storage is unavailable." }
        val attributes = mapOf<Any?, Any?>(kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        var lastStatus = errSecSuccess
        try { writeKeychainValue(
            update = {
                baseQueryMap(account).useCFDictionary { query ->
                    attributes.useCFDictionary { changes -> SecItemUpdate(query, changes).also { lastStatus = it }.writeResult() }
                }
            },
            add = { (baseQueryMap(account) + attributes).useCFDictionary { SecItemAdd(it, null).also { lastStatus = it }.writeResult() } },
        ) } catch (_: IllegalStateException) { throw KeychainStorageException("write", lastStatus) }
    }

    suspend fun get(account: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        result.value = null
        try {
            val status = (baseQueryMap(account) + mapOf<Any?, Any?>(
                kSecReturnData to true, kSecMatchLimit to kSecMatchLimitOne,
            )).useCFDictionary { SecItemCopyMatching(it, result.ptr) }
            when (status) {
                errSecSuccess -> {
                    val retained = result.value
                    result.value = null // Transfer the retained result to ARC exactly once.
                    val data = CFBridgingRelease(retained) as? NSData
                    checkNotNull(data?.toUtf8String()) { "Secure session storage is unavailable." }
                }
                errSecItemNotFound -> null
                else -> throw KeychainStorageException("read", status)
            }
        } finally { result.value?.let { CFRelease(it) } }
    }

    suspend fun clear(account: String) {
        val status = baseQueryMap(account).useCFDictionary { SecItemDelete(it) }
        if (status != errSecSuccess && status != errSecItemNotFound) throw KeychainStorageException("delete", status)
    }

    private fun Int.writeResult(): KeychainWriteResult = when (this) {
        errSecSuccess -> KeychainWriteResult.Success
        errSecItemNotFound -> KeychainWriteResult.NotFound
        errSecDuplicateItem -> KeychainWriteResult.Duplicate
        else -> KeychainWriteResult.Failed
    }

    private fun baseQueryMap(account: String): Map<Any?, Any?> =
        mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to account,
        )
}

@OptIn(ExperimentalForeignApi::class)
private inline fun <T> Map<Any?, Any?>.useCFDictionary(block: (CFDictionaryRef) -> T): T {
    val dictionary = toRetainedCFDictionary()
    return try {
        block(dictionary)
    } finally {
        CFRelease(dictionary)
    }
}

/** Preserve native CF constants; boxing their pointers into NSDictionary produces errSecParam. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun Map<Any?, Any?>.toRetainedCFDictionary(): CFDictionaryRef {
    val dictionary = checkNotNull(CFDictionaryCreateMutable(null, 0,
        kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr))
    try {
        forEach { (key, value) ->
            if (key != null && value != null) {
                val (nativeKey, releaseKey) = key.asCFValue()
                val (nativeValue, releaseValue) = value.asCFValue()
                try { CFDictionarySetValue(dictionary, nativeKey, nativeValue) }
                finally {
                    if (releaseKey) CFRelease(nativeKey)
                    if (releaseValue) CFRelease(nativeValue)
                }
            }
        }
        return dictionary
    } catch (error: Throwable) { CFRelease(dictionary); throw error }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun Any.asCFValue(): Pair<CFTypeRef?, Boolean> = when (this) {
    is CPointer<*> -> this to false
    is Boolean -> (if (this) kCFBooleanTrue else kCFBooleanFalse) to false
    else -> CFBridgingRetain(if (this is String) NSString.create(string = this) else this) to true
}

internal class KeychainStorageException(val operation: String, val status: Int) :
    IllegalStateException("Secure session storage is unavailable. Please try again.")

@OptIn(BetaInteropApi::class)
private fun String.toNSData(): NSData? =
    NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

@OptIn(BetaInteropApi::class)
private fun NSData.toUtf8String(): String? =
    NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()
