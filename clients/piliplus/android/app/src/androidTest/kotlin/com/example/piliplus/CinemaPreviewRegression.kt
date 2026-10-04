package com.example.piliplus

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Offline production Preview regression. No Flutter account, network media or XR claims. */
class CinemaPreviewRegression : Instrumentation() {
    private var activity: CinemaPreviewActivity? = null
    private var qualityReply: ((Map<String, Any>?) -> Unit)? = null
    private var catalogReply: ((Map<String, Any>?) -> Unit)? = null
    private var holdCatalog = false
    private val steps = mutableListOf<String>()
    private lateinit var fixture: File
    private lateinit var qualityFixture: File

    override fun onCreate(args: Bundle?) { super.onCreate(args); start() }
    private fun <T> main(block: () -> T): T {
        val value = AtomicReference<T>()
        val failure = AtomicReference<Throwable>()
        runOnMainSync { try { value.set(block()) } catch (t: Throwable) { failure.set(t) } }
        failure.get()?.let { throw it }
        return value.get()
    }
    private fun player(): ExoPlayer {
        val field = CinemaPreviewActivity::class.java.getDeclaredField("player")
        field.isAccessible = true
        return field.get(activity) as ExoPlayer
    }
    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun click(symbol: String) = main {
        val control = views(activity!!.window.decorView).filterIsInstance<CinemaControl>().first { it.symbol == symbol }
        check(control.performClick()) { "Control did not dispatch: $symbol" }
    }
    private fun chooseQuality() = main {
        qualityReply = null
        val item = views(activity!!.window.decorView).filterIsInstance<TextView>().first { it.contentDescription?.toString() == "离线同源回归" }
        check(item.performClick()) { "Quality item did not dispatch" }
        checkNotNull(qualityReply) { "Quality selection did not issue a fresh request" }
    }
    private fun catalog(): Map<String, Any> = mapOf("qualities" to listOf(mapOf("id" to 80, "title" to "离线同源回归", "selected" to false)))
    private fun media(cid: Long, quality: Boolean = false): Map<String, Any> = mapOf("video" to (if (quality) qualityFixture else fixture).toURI().toString(), "cid" to cid, "headers" to emptyMap<String, String>())
    private fun waitFor(label: String, predicate: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < deadline) {
            if (main(predicate)) return
            SystemClock.sleep(100)
        }
        error("Timed out: $label")
    }
    private fun launch(cid: Long) {
        main {
            CinemaSession.media = CinemaMedia(fixture.toURI().toString(), null, cid, emptyMap(), 0)
            CinemaSession.title = "离线原生回归测试"
            CinemaSession.request = { method, _, done ->
                when (method) {
                    "catalog" -> if (holdCatalog) catalogReply = done else done(catalog())
                    "quality" -> qualityReply = done
                    else -> done(null)
                }
            }
        }
        activity = startActivitySync(Intent(targetContext, CinemaPreviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as CinemaPreviewActivity
        waitFor("decoded local fixture") { player().videoSize.width > 0 && player().isPlaying && player().currentPosition > 250 }
    }
    override fun onStart() {
        val result = Bundle()
        var passed = false
        try {
            check(Build.MODEL.contains("Spatial Simulator")) { "Only an isolated Spatial Simulator is supported" }
            fixture = File(targetContext.filesDir, "preview-regression.mp4")
            context.assets.open("playback-h264-aac.mp4").use { input -> fixture.outputStream().use { input.copyTo(it) } }
            qualityFixture = File(targetContext.filesDir, "preview-regression-quality.mp4")
            fixture.copyTo(qualityFixture, overwrite = true)
            launch(1)
            steps += "real_exoplayer_local_decode"
            click("pause")
            waitFor("pause") { !player().isPlaying }
            main { player().seekTo(4000) }
            waitFor("paused seek to four seconds") { player().playbackState == androidx.media3.common.Player.STATE_READY && player().currentPosition in 3700L..4300L }
            val pausedAt = main { player().currentPosition }
            SystemClock.sleep(600)
            check(main { !player().isPlaying && kotlin.math.abs(player().currentPosition - pausedAt) < 400 })
            click("quality"); chooseQuality()
            main {
                val before = CinemaSession.media
                checkNotNull(qualityReply).invoke(media(1, quality = true))
                check(CinemaSession.media !== before) { "Live quality reply did not replace the media" }
            }
            waitFor("paused quality reload") { player().playbackState == androidx.media3.common.Player.STATE_READY && player().videoSize.width > 0 && player().currentMediaItem?.localConfiguration?.uri?.toString() == qualityFixture.toURI().toString() }
            check(main { !player().playWhenReady && !player().isPlaying && kotlin.math.abs(player().currentPosition - pausedAt) < 400 })
            steps += "paused_quality_reload_preserves_position_and_pause"
            checkNotNull(uiAutomation.takeScreenshot()).let { bitmap ->
                val screenshot = File(targetContext.filesDir, "native-preview-offline.png")
                screenshot.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) { "Screenshot encoding failed" } }
                bitmap.recycle()
                checkNotNull(android.graphics.BitmapFactory.decodeFile(screenshot.absolutePath)) { "Saved screenshot cannot decode" }.recycle()
            }
            click("quality"); chooseQuality()
            val oldQuality = main { checkNotNull(qualityReply) }
            val ending = activity!!
            main {
                val before = CinemaSession.media
                ending.finish()
                oldQuality(media(999)) // Deliberately before onDestroy in this same UI task.
                check(CinemaSession.media === before) { "finish-before-destroy callback changed the session" }
            }
            waitFor("first session destroyed") { ending.isDestroyed && CinemaSession.media == null }
            steps += "finish_before_destroy_rejects_quality_reply"
            launch(2)
            main { oldQuality(media(999)); check(CinemaSession.media!!.cid == 2L) }
            check(main { player().isPlaying })
            steps += "old_session_reply_cannot_mutate_new_session"
            click("quality"); chooseQuality()
            main {
                callActivityOnPause(activity)
                checkNotNull(qualityReply).invoke(media(2, quality = true))
                check(!player().playWhenReady) { "Background reply restarted playback" }
                callActivityOnResume(activity)
            }
            waitFor("background quality ready") { player().playbackState == androidx.media3.common.Player.STATE_READY && player().currentMediaItem?.localConfiguration?.uri?.toString() == qualityFixture.toURI().toString() }
            check(main { !player().isPlaying && !player().playWhenReady })
            steps += "onPause_quality_reply_does_not_restart_playback"
            main { holdCatalog = true }
            click("quality")
            val last = activity!!
            main {
                val scroll = last.window.decorView.findViewWithTag<ViewGroup>("quality-menu")
                val panel = checkNotNull(scroll).getChildAt(0) as ViewGroup
                val count = panel.childCount
                last.finish()
                checkNotNull(catalogReply).invoke(catalog())
                check(panel.childCount == count) { "Late catalog populated closing menu" }
            }
            waitFor("final session destroyed") { last.isDestroyed && CinemaSession.media == null }
            steps += "finish_before_destroy_rejects_catalog_reply"
            passed = true
        } catch (t: Throwable) {
            result.putString("error", t.toString())
        } finally {
            main { activity?.let { if (!it.isFinishing) it.finish() }; CinemaSession.request = null }
            result.putBoolean("passed", passed)
            result.putString("steps", steps.joinToString("\n"))
            result.putString("coverage", "Native Preview, real ExoPlayer, UI callbacks and injected lifecycle; excludes Flutter MediaKit return, XR and headset")
            finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
        }
    }
}
