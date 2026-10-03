package com.example.piliplus

/** Ephemeral handoff. Media credentials and signed URLs are never persisted. */
data class SpatialMedia(
    val video: String,
    val audio: String?,
    val title: String,
    val cid: Long,
    val headers: Map<String, String>,
    val positionMs: Long,
)

object SpatialSession {
    var testCommand: ((String) -> Unit)? = null
    var request: ((String, Map<String, Any>, (Map<String, Any>?) -> Unit) -> Unit)? = null
    var media: SpatialMedia? = null
    var finish: ((Long, Long) -> Unit)? = null
    fun complete(position: Long) {
        val callback = finish
        val cid = media?.cid ?: 0L
        finish = null
        media = null
        callback?.invoke(position, cid)
    }
}
