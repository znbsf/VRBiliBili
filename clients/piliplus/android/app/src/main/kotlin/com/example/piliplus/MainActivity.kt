package com.example.piliplus

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams
import com.ryanheise.audioservice.AudioServiceActivity
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : AudioServiceActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "vrbilibili/spatial")
        SpatialSession.request = { method, args, done ->
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
                if (call.method == "isQuest") {
                    result.success(Build.MODEL.contains("Quest", ignoreCase = true))
                } else if (call.method == "debugHandoff" && applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    filesDir.resolve("spatial-handoff.json").writeText(org.json.JSONObject(mapOf(
                        "positionMs" to call.argument<Number>("positionMs"),
                        "cid" to call.argument<Number>("cid"),
                        "playing" to call.argument<Boolean>("playing"),
                    )).toString())
                    result.success(null)
                } else if (call.method != "open") {
                    result.notImplemented()
                } else if (SpatialSession.media != null) {
                    result.error("busy", "空间播放器已打开", null)
                } else if (!Build.MODEL.contains("Quest", ignoreCase = true)) {
                    result.error("device", "空间模式需要 Quest 头显", null)
                } else {
                    val video = call.argument<String>("video")
                    if (video.isNullOrBlank()) {
                        result.error("media", "视频尚未加载", null)
                    } else {
                        SpatialSession.media = SpatialMedia(
                            video, call.argument<String>("audio"),
                            call.argument<String>("title") ?: "VRBiliBili",
                            call.argument<Number>("cid")?.toLong() ?: 0L,
                            call.argument<Map<String, String>>("headers") ?: emptyMap(),
                            call.argument<Number>("positionMs")?.toLong() ?: 0L,
                        )
                        SpatialSession.finish = { position, cid ->
                            result.success(mapOf("positionMs" to position, "cid" to cid))
                        }
                        try {
                            startActivity(Intent(this, SpatialPlayerActivity::class.java).apply {
                                action = Intent.ACTION_MAIN
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e: Exception) {
                            SpatialSession.media = null
                            SpatialSession.finish = null
                            result.error("launch", "空间模式启动失败", null)
                        }
                    }
                }
            }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (AndroidHelper.isFoldable) {
            AndroidHelper.ToDart.onConfigurationChanged?.run()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (SpatialSession.media == null) AndroidHelper.ToDart.onUserLeaveHint?.run()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        AndroidHelper.isPipMode = isInPictureInPictureMode
    }
}
