package com.example.piliplus

/** Reject callbacks from an older request, destroyed preview, or replaced session. */
class CinemaCallbackGuard(private val sessionGeneration: Long) {
    private var epoch = 0L
    private var closed = false

    fun begin(): Long = ++epoch

    fun accepts(
        requestEpoch: Long,
        currentSessionGeneration: Long,
        finishing: Boolean = false,
        destroyed: Boolean = false,
    ): Boolean = !closed && !finishing && !destroyed &&
        requestEpoch == epoch && currentSessionGeneration == sessionGeneration

    fun close() {
        closed = true
        ++epoch
    }
}
