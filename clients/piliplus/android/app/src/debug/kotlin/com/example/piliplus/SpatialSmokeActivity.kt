package com.example.piliplus

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** Debug APK only, shell permission required. Always uses our original test fixture. */
class SpatialSmokeActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        SpatialSession.media = SpatialMedia("asset:///playback-h264-aac.mp4", null,
            "VRBiliBili 原创测试片", 0, emptyMap(), 0)
        SpatialSession.request = { _, _, done -> done(emptyMap()) }
        SpatialSession.finish = { position, _ ->
            filesDir.resolve("spatial-return.txt").writeText(position.toString())
        }
        startActivity(Intent(this, SpatialPlayerActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }
}

class SpatialSmokeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.i("VRBiliSpatialTest", "command=${intent.getStringExtra("command")} active=${SpatialSession.testCommand != null}")
        SpatialSession.testCommand?.invoke(intent.getStringExtra("command") ?: "status")
    }
}
