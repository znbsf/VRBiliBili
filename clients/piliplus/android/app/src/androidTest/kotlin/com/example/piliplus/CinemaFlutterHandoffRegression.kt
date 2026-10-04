package com.example.piliplus

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.media3.exoplayer.ExoPlayer
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real production download route, MediaKit, MethodChannel and native Preview.
 * Uses only owned six-second fixture files in a fresh private Spatial Simulator.
 * Does not replace CinemaSession.request or emulate a Flutter player.
 */
class CinemaFlutterHandoffRegression : Instrumentation() {
    private val firstCid = 9000001L
    private val secondCid = 9000002L
    private val steps = mutableListOf<String>()
    private var preview: CinemaPreviewActivity? = null
    private var mainActivity: Activity? = null

    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun callActivityOnResume(activity: Activity) {
        super.callActivityOnResume(activity)
        if (activity is CinemaPreviewActivity) preview = activity
    }
    private fun <T> main(block: () -> T): T {
        val value = AtomicReference<T>(); val error = AtomicReference<Throwable>()
        runOnMainSync { try { value.set(block()) } catch (t: Throwable) { error.set(t) } }
        error.get()?.let { throw it }; return value.get()
    }
    private fun fixture(cid: Long, suffix: String) {
        val folder = File(checkNotNull(targetContext.getExternalFilesDir(null)), "download/$cid/c_$cid")
        val media = File(folder, "80/0.mp4"); check(media.parentFile!!.mkdirs() || media.parentFile!!.isDirectory)
        context.assets.open("playback-h264-aac.mp4").use { input -> media.outputStream().use { input.copyTo(it) } }
        val page = JSONObject().put("cid", cid).put("page", 1).put("part", "回归片段$suffix")
            .put("has_alias", false).put("tid", 0).put("width", 240).put("height", 136).put("rotate", 0)
        val entry = JSONObject().put("media_type", 1).put("has_dash_audio", false).put("is_completed", true)
            .put("total_bytes", media.length()).put("downloaded_bytes", media.length()).put("title", "VRBiliBili 离线回归 $suffix")
            .put("type_tag", "80").put("cover", "").put("video_quality", 80).put("prefered_video_quality", 80)
            .put("quality_pithy_description", "原创离线fixture").put("guessed_total_bytes", media.length())
            .put("total_time_milli", 6016).put("danmaku_count", 0).put("time_update_stamp", cid)
            .put("time_create_stamp", cid).put("can_play_in_advance", false).put("interrupt_transform_temp_file", false)
            .put("avid", cid).put("bvid", "BV1Offline$suffix").put("page_data", page)
        File(folder, "entry.json").writeText(entry.toString())
    }
    private fun find(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = "${node.contentDescription ?: ""} ${node.text ?: ""}"
        if (node.isClickable && text.contains(label)) return node
        for (index in 0 until node.childCount) find(node.getChild(index), label)?.let { return it }
        return null
    }
    private fun awaitNode(label: String): AccessibilityNodeInfo {
        val end = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < end) {
            find(uiAutomation.rootInActiveWindow, label)?.let { return it }
            SystemClock.sleep(200)
        }
        error("Missing UI action: $label")
    }
    private fun click(label: String) {
        check(awaitNode(label).performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Click rejected: $label" }
        SystemClock.sleep(300)
    }
    private fun state(method: String = "playerState"): JSONObject {
        val done = CountDownLatch(1); val response = AtomicReference<JSONObject>()
        main {
            checkNotNull(MainActivity.debugChannel).invokeMethod(method, mapOf("probeCid" to firstCid), object : MethodChannel.Result {
                override fun success(value: Any?) { if (value is Map<*, *>) response.set(JSONObject(value)); done.countDown() }
                override fun error(code: String, message: String?, details: Any?) { done.countDown() }
                override fun notImplemented() { done.countDown() }
            })
        }
        check(done.await(5, TimeUnit.SECONDS)) { "Flutter state response timed out" }
        return checkNotNull(response.get()) { "Flutter state unavailable" }
    }
    private fun awaitState(label: String, checkState: (JSONObject) -> Boolean): JSONObject {
        val end = SystemClock.uptimeMillis() + 25000
        var last = JSONObject()
        while (SystemClock.uptimeMillis() < end) {
            last = state(); if (checkState(last)) return last; SystemClock.sleep(150)
        }
        error("State timeout: $label: $last")
    }
    private fun player(): ExoPlayer {
        val field = CinemaPreviewActivity::class.java.getDeclaredField("player")
        field.isAccessible = true; return field.get(checkNotNull(preview)) as ExoPlayer
    }
    private fun awaitNative(label: String, predicate: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 25000
        while (SystemClock.uptimeMillis() < end) {
            if (main(predicate)) return; SystemClock.sleep(100)
        }
        error("Native timeout: $label")
    }
    private fun capture(name: String) {
        val bitmap = checkNotNull(uiAutomation.takeScreenshot())
        val file = File(targetContext.filesDir, name)
        file.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle(); checkNotNull(android.graphics.BitmapFactory.decodeFile(file.path)).recycle()
    }
    private fun openOffline(suffix: String, cid: Long): JSONObject {
        click("我的"); click("离线缓存"); click("VRBiliBili 离线回归 $suffix")
        return awaitState("MediaKit decoded fixture $suffix") {
            it.optLong("cid") == cid && it.optString("dataStatus") == "loaded" &&
                !it.optBoolean("processing") && it.optBoolean("rawPlaying") && it.optInt("width") > 0 && it.optLong("positionMs") > 400
        }
    }
    private fun enter(cid: Long, expectedPosition: Long? = null) {
        preview = null; click("全屏影院")
        awaitNative("production native decode") {
            preview != null && CinemaSession.media?.cid == cid && player().videoSize.width > 0 &&
                player().isPlaying
        }
        if (expectedPosition != null) main {
            check(kotlin.math.abs(checkNotNull(CinemaSession.media).positionMs - expectedPosition) <= 400) { "Reentry handed off wrong position" }
            check(player().currentPosition in (expectedPosition - 400)..(expectedPosition + 1500)) { "Reentry decoded at wrong position: ${player().currentPosition}, expected $expectedPosition" }
        }
        val flutter = state()
        check(flutter.optBoolean("cinemaActive") && !flutter.optBoolean("rawPlaying")) { "Two players active: $flutter" }
    }
    private fun closeAndVerify(cid: Long): JSONObject {
        click("暂停"); awaitNative("native paused") { !player().isPlaying }
        val position = main { player().currentPosition }
        check(position in 1000L..5500L) { "Fixture ended before return: $position" }
        SystemClock.sleep(500); capture("flutter-native-preview.png")
        val generation = main { CinemaSession.generation }
        click("退出全屏")
        val returned = awaitState("restored MediaKit paused at native position") {
            !it.optBoolean("cinemaActive") && !it.optBoolean("processing") && it.optString("dataStatus") == "loaded" &&
                it.optLong("cid") == cid && !it.optBoolean("rawPlaying") && !it.optBoolean("playing") &&
                kotlin.math.abs(it.optLong("positionMs") - position) <= 400 &&
                kotlin.math.abs(it.optLong("cachedPositionMs", -1) - position) <= 400 &&
                kotlin.math.abs(it.optLong("playedTimeMs", -1) - position) <= 400
        }
        check(main { CinemaSession.media == null && CinemaSession.generation > generation }) { "Native session not drained" }
        SystemClock.sleep(650)
        val stable = state()
        check(!stable.optBoolean("rawPlaying") && !stable.optBoolean("playing") &&
            kotlin.math.abs(stable.optLong("positionMs") - returned.optLong("positionMs")) <= 200) { "Return resumed late: $stable" }
        awaitNode("全屏影院"); return returned
    }
    override fun onStart() {
        val result = Bundle(); var passed = false
        try {
            check(android.os.Build.MODEL.contains("Spatial Simulator")) { "Fresh private Spatial Simulator required" }
            fixture(firstCid, "A"); fixture(secondCid, "B")
            mainActivity = startActivitySync(Intent(targetContext, MainActivity::class.java).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            openOffline("A", firstCid); steps += "production_download_route_and_real_mediakit_decode"
            val guards = state("verifyCinemaEntryGuards")
            check(guards.optBoolean("querying") && guards.optBoolean("mismatchedCid") && !guards.optBoolean("cinemaActive")) { "Pending CID guard failed: $guards" }
            check(state().optLong("detailCid") == firstCid) { "Guard probe did not restore page CID" }
            steps += "real_cinema_open_rejects_pending_query_and_mismatched_detail_cid"
            capture("flutter-offline-before.png")
            enter(firstCid); steps += "real_flutter_methodchannel_to_native_only_one_player"
            awaitNative("native position advances") { player().currentPosition >= 2500 }
            val first = closeAndVerify(firstCid); steps += "native_return_restores_paused_mediakit_position_and_current_process_cached_progress=$first"
            capture("flutter-offline-return.png")
            val oldPreview = preview; val oldGeneration = main { CinemaSession.generation }
            enter(firstCid, first.optLong("positionMs"))
            check(preview !== oldPreview && main { CinemaSession.generation > oldGeneration }) { "Reentry reused closing native session" }
            closeAndVerify(firstCid); steps += "new_native_session_reenters_at_returned_position_and_closes_cleanly"
            val firstCache = state().optLong("cachedPositionMs")
            click("返回主页"); openOffline("B", secondCid)
            val second = state()
            // Closing A's page persists its decoded seek position (frame quantization).
            // Establish the stable old-CID baseline after that normal dispose write.
            val firstCacheAfterNavigation = second.optLong("cachedProbePositionMs", -1)
            check(second.optLong("detailCid") == secondCid && firstCacheAfterNavigation >= 0 &&
                kotlin.math.abs(firstCacheAfterNavigation - firstCache) <= 400) { "CID switch changed old cache: old=$firstCache, new=$second" }
            check(kotlin.math.abs(firstCache - second.optLong("positionMs")) > 800) { "CID test positions must be distinct: old=$firstCache, second=$second" }
            steps += "second_download_cid_does_not_overwrite_first_progress: before=$firstCache after=$firstCacheAfterNavigation second=${second.optLong("positionMs")}"
            enter(secondCid); awaitNative("second native advance") { player().currentPosition >= 2000 }
            val secondReturn = closeAndVerify(secondCid)
            check(secondReturn.optLong("cachedProbePositionMs") == firstCacheAfterNavigation) { "Second return overwrote first CID cache" }
            steps += "second_cid_native_return_is_scoped_and_preserves_first_cache=$secondReturn"
            passed = true
        } catch (t: Throwable) {
            result.putString("error", t.toString())
            try { capture("flutter-handoff-failure.png") } catch (_: Throwable) {}
            val labels = mutableListOf<String>()
            fun collect(node: AccessibilityNodeInfo?) {
                if (node == null || labels.size > 70) return
                val text = "${node.contentDescription ?: ""} ${node.text ?: ""}".trim()
                if (text.isNotEmpty()) labels += text.take(160)
                for (index in 0 until node.childCount) collect(node.getChild(index))
            }
            collect(uiAutomation.rootInActiveWindow); result.putString("visible_labels", labels.toString())
            result.putString("active_window_package", uiAutomation.rootInActiveWindow?.packageName?.toString())
        } finally {
            main { preview?.let { if (!it.isFinishing) it.finish() } }
            result.putBoolean("passed", passed); result.putString("steps", steps.joinToString("\n"))
            result.putString("coverage", "Real production offline Flutter/MediaKit/MethodChannel/native Preview/return/CID; no DASH quality, network video, XR or headset")
            finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
        }
    }
}
