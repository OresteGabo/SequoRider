package dev.orestegabo.sequorider.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal fun riderWorkspaceScope(parent: CoroutineScope): CoroutineScope =
    CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))

/** A cancelled account request must never publish its late response or become an offline error. */
internal suspend inline fun <T> riderRequest(block: () -> T): Result<T> = try {
    val value = block()
    coroutineContext.ensureActive()
    Result.success(value)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
