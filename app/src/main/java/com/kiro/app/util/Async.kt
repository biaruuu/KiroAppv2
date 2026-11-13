package com.kiro.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs blocking [block] (the Java data layer) on the IO dispatcher and returns the outcome as a [Result].
 * Coroutine cancellation is re-thrown, never swallowed, so debounced searches cancel cleanly.
 */
suspend inline fun <T> ioCall(crossinline block: () -> T): Result<T> =
    try {
        Result.success(withContext(Dispatchers.IO) { block() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** First sentence of a synopsis, trimmed to [max] characters - used as the hero tagline. */
fun String.tagline(max: Int = 90): String {
    val s = trim().replace('\n', ' ')
    if (s.isEmpty()) return s
    val end = s.indexOfFirst { it == '.' || it == '!' || it == '?' }
    return if (end in 20..max) s.substring(0, end + 1)
    else if (s.length <= max) s
    else s.take(max).trimEnd() + "…"
}
