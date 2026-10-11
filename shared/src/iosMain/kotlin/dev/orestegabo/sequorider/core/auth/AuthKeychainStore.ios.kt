package dev.orestegabo.sequorider.core.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
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

internal class AuthKeychainStore(
    private val service: String = "dev.orestegabo.sequorider.auth",
) {
    suspend fun save(account: String, value: String) {
        val data = value.toNSData() ?: return
        baseQueryMap(account).useCFDictionary { SecItemDelete(it) }
        (baseQueryMap(account) + mapOf<Any?, Any?>(kSecValueData to data)).useCFDictionary {
            SecItemAdd(it, null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    suspend fun get(account: String): String? =
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = (baseQueryMap(account) + mapOf<Any?, Any?>(
                kSecReturnData to true,
                kSecMatchLimit to kSecMatchLimitOne,
            )).useCFDictionary { query ->
                SecItemCopyMatching(query, result.ptr)
            }
            when (status) {
                errSecSuccess -> (result.value as? NSData)?.toUtf8String()
                errSecItemNotFound -> null
                else -> null
            }.also {
                result.value?.let { value -> CFRelease(value) }
            }
        }

    suspend fun clear(account: String) {
        baseQueryMap(account).useCFDictionary { SecItemDelete(it) }
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

@OptIn(ExperimentalForeignApi::class)
private fun Map<Any?, Any?>.toRetainedCFDictionary(): CFDictionaryRef {
    val dictionary = NSMutableDictionary()
    forEach { (key, value) ->
        if (key != null && value != null) {
            dictionary.setObject(value, forKey = key as platform.Foundation.NSCopyingProtocol)
        }
    }
    return CFBridgingRetain(dictionary) as CFDictionaryRef
}

@OptIn(BetaInteropApi::class)
private fun String.toNSData(): NSData? =
    (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)

@OptIn(BetaInteropApi::class)
private fun NSData.toUtf8String(): String? =
    NSString.create(this, NSUTF8StringEncoding) as String?
