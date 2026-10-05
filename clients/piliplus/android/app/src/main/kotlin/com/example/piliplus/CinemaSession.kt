package com.example.piliplus

import android.app.Activity
import android.content.Intent
import android.os.Build
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/** Signed media URLs stay in memory. Flutter owns account and quality resolution. */
data class CinemaMedia(val video: String, val audio: String?, val cid: Long,
    val headers: Map<String, String>, val positionMs: Long)

object CinemaSession {
    var generation: Long = 0L
        private set
    var media: CinemaMedia? = null
    var title: String = ""
    var request: ((String, Map<String, Any>, (Map<String, Any>?) -> Unit) -> Unit)? = null
    var testCommand: ((String) -> Unit)? = null
    private var pending: MethodChannel.Result? = null

    fun configure(activity: Activity, engine: FlutterEngine) {
        val channel = MethodChannel(engine.dartExecutor.binaryMessenger, "vrbilibili/cinema")
        request = { method, args, done ->
            channel.invokeMethod(method, args, object : MethodChannel.Result {
                override fun success(result: Any?) {
                    @Suppress("UNCHECKED_CAST")
                    done(result as? Map<String, Any>)
                }
                override fun error(code: String, message: String?, details: Any?) { done(null) }
                override fun notImplemented() { done(null) }
            })
        }
        channel.setMethodCallHandler { call, result ->
            if (call.method != "open") result.notImplemented()
            else if (pending != null) result.error("busy", "影院已经打开", null)
            else if (!Build.MODEL.contains("Quest", true) && !Build.MODEL.contains("Spatial Simulator", true)) result.error("device", "需要 Quest 头显", null)
            else {
                val video = call.argument<String>("video")
                if (video.isNullOrBlank()) result.error("media", "视频尚未加载", null)
                else {
                    media = CinemaMedia(video, call.argument<String>("audio"),
                        call.argument<Number>("cid")?.toLong() ?: 0L,
                        call.argument<Map<String, String>>("headers") ?: emptyMap(),
                        call.argument<Number>("positionMs")?.toLong() ?: 0L)
                    title = call.argument<String>("title") ?: ""
                    pending = result
                    ++generation
                    try {
                        activity.startActivity(Intent(activity, if(Build.MODEL.contains("Spatial Simulator", true)) CinemaPreviewActivity::class.java else CinemaActivity::class.java).apply {
                            action = Intent.ACTION_MAIN
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {
                        pending = null; media = null
                        ++generation
                        result.error("launch", "影院启动失败", null)
                    }
                }
            }
        }
    }

    fun complete(position: Long) {
        val result = pending
        val cid = media?.cid ?: 0L
        pending = null; media = null; title = ""; testCommand = null
        ++generation
        result?.success(mapOf("positionMs" to position, "cid" to cid))
    }
}
