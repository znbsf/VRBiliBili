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
class CinemaCacheRecoveryRegression : Instrumentation() {
    private val firstCid = 9000001L
    private val secondCid = 9000002L
    private val steps = mutableListOf<String>()
    private var preview: CinemaPreviewActivity? = null
    private var mainActivity: Activity? = null

    private var phase = "seed"
    override fun onCreate(arguments: Bundle?) {
        phase = arguments?.getString("phase") ?: "seed"
        super.onCreate(arguments); start()
    }
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
            val root = uiAutomation.rootInActiveWindow
            // Spatial Simulator's factory Bluetooth service can ANR independently.
            // Dismiss its Wait dialog only in the fresh owned simulator; no settings.
            if (root?.findAccessibilityNodeInfosByText("Bluetooth isn't responding")?.isNotEmpty() == true) {
                find(root, "Wait")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                steps += "owned_simulator_bluetooth_anr_wait_dialog_dismissed"
                SystemClock.sleep(300)
                continue
            }
            find(root, label)?.let { return it }
            SystemClock.sleep(200)
        }
        error("Missing UI action: $label")
    }
    private fun click(label: String) {
        check(awaitNode(label).performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Click rejected: $label" }
        SystemClock.sleep(300)
    }
    private fun state(method: String = "playerState", probeCid: Long = firstCid): JSONObject {
        val done = CountDownLatch(1); val response = AtomicReference<JSONObject>()
        main {
            checkNotNull(MainActivity.debugChannel).invokeMethod(method, mapOf("probeCid" to probeCid), object : MethodChannel.Result {
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
    private fun folder(cid: Long) = File(checkNotNull(targetContext.getExternalFilesDir(null)), "download/$cid/c_$cid")
    private fun edit(cid: Long, block: (JSONObject, File) -> Unit) {
        val dir = folder(cid)
        val jsonFile = File(dir, "entry.json")
        val data = JSONObject(jsonFile.readText())
        block(data, dir)
        jsonFile.writeText(data.toString())
    }
    private fun catalogFixtures() {
        // Every record and media byte belongs to this test's fresh app sandbox.
        for (cid in 9000010L..9000022L) fixture(cid, "X$cid")
        File(folder(9000010), "entry.json").writeText("{broken-json")
        edit(9000011) { json, _ -> json.remove("page_data") }
        edit(9000012) { json, _ -> json.remove("type_tag") }
        edit(9000013) { _, dir -> check(File(dir, "80/0.mp4").delete()) }
        edit(9000014) { _, dir -> File(dir, "80/0.mp4").writeBytes(byteArrayOf()) }
        edit(9000015) { json, _ -> json.put("is_completed", false).put("downloaded_bytes", 7) }
        edit(9000016) { json, dir ->
            json.put("media_type", 2)
            check(File(dir, "80/0.mp4").renameTo(File(dir, "80/video.m4s")))
        }
        edit(9000017) { json, dir ->
            json.put("media_type", 2).put("has_dash_audio", true)
            check(File(dir, "80/0.mp4").renameTo(File(dir, "80/video.m4s")))
        }
        edit(9000018) { json, dir ->
            json.put("media_type", 2).put("has_dash_audio", true)
            File(dir, "80/0.mp4").copyTo(File(dir, "80/audio.m4s"))
            check(File(dir, "80/0.mp4").renameTo(File(dir, "80/video.m4s")))
        }
        edit(9000019) { json, _ ->
            json.remove("page_data")
            json.put("source", JSONObject().put("av_id", 9000019).put("cid", 9000019))
            json.put("season_id", "9000019")
            json.put("ep", JSONObject().put("av_id", 9000019).put("page", 0).put("danmaku", 9000019)
                .put("cover", "").put("episode_id", 9000019).put("index", "1").put("index_title", "Owned PGC metadata")
                .put("from", "bangumi").put("season_type", 0).put("width", 0).put("height", 0)
                .put("rotate", 0).put("link", "").put("bvid", "BV1OfflinePGC").put("sort_index", 0))
        }
        edit(9000020) { json, _ -> json.put("media_type", 99) }
        edit(9000021) { json, _ -> json.put("type_tag", "../80") }
        edit(9000022) { json, _ -> json.put("title", 123) }
    }
    private fun pausedState(probeCid: Long = firstCid): JSONObject {
        state("pauseForCacheRecovery")
        return state(probeCid = probeCid)
    }
    private fun verifyRestore(suffix: String, cid: Long, expected: Long, other: Long, otherExpected: Long): JSONObject {
        openOffline(suffix, cid)
        val current = pausedState(other)
        check(current.optLong("cachedPositionMs", -1) == expected) { "Cache lost on process restart: $current expected $expected" }
        check(current.optLong("cachedProbePositionMs", -1) == otherExpected) { "Other CID changed on startup: $current" }
        check(current.optLong("positionMs") in (expected - 400)..(expected + 1800)) { "Decoder did not seek to persisted progress: $current expected $expected" }
        return current
    }
    override fun onStart() {
        val result = Bundle(); var passed = false
        try {
            check(android.os.Build.MODEL.contains("Spatial Simulator")) { "Fresh private Spatial Simulator required" }
            if (phase == "seed") { fixture(firstCid, "A"); fixture(secondCid, "B") }
            if (phase == "catalog") catalogFixtures()
            mainActivity = startActivitySync(Intent(targetContext, MainActivity::class.java).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val seedFile = File(targetContext.filesDir, "cache-recovery-seed.json")
            when (phase) {
                "seed" -> {
                    openOffline("A", firstCid)
                    val guards = state("verifyCinemaEntryGuards")
                    for (name in listOf("querying", "mismatchedCid", "none", "loading", "error")) {
                        check(guards.optBoolean(name)) { "Cinema accepted unfinished source: $guards" }
                    }
                    check(!guards.optBoolean("cinemaActive"))
                    steps += "production_cinema_open_rejects_pending_cid_and_none_loading_error_states=$guards"
                    enter(firstCid); awaitNative("A native progress") { player().currentPosition >= 2400 }
                    val first = closeAndVerify(firstCid)
                    click("返回主页"); openOffline("B", secondCid)
                    val baselineA = state().optLong("cachedProbePositionMs", -1)
                    check(kotlin.math.abs(baselineA - first.optLong("cachedPositionMs")) <= 400)
                    enter(secondCid); awaitNative("B native progress") { player().currentPosition >= 1200 }
                    val second = closeAndVerify(secondCid)
                    check(second.optLong("cachedProbePositionMs", -1) == baselineA) { "B wrote A's cache: $second" }
                    check(kotlin.math.abs(baselineA - second.optLong("cachedPositionMs")) > 700) { "Distinct CIDs need distinct positions" }
                    val seed = JSONObject().put("firstCid", firstCid).put("secondCid", secondCid)
                        .put("firstMs", baselineA).put("secondMs", second.optLong("cachedPositionMs"))
                        .put("watchProgressPath", second.getString("watchProgressPath")).put("pid", android.os.Process.myPid())
                    seedFile.writeText(seed.toString())
                    steps += "real_native_return_waited_for_hive_backend_and_second_cid_preserved_first=$seed"
                }
                "restart", "torn" -> {
                    val seed = JSONObject(seedFile.readText())
                    check(seed.getInt("pid") != android.os.Process.myPid()) { "Same process cannot prove persistence" }
                    val first = verifyRestore("A", firstCid, seed.getLong("firstMs"), secondCid, seed.getLong("secondMs"))
                    steps += "new_process_restored_actual_mediakit_A=$first"
                    click("返回主页")
                    openOffline("B", secondCid)
                    val second = pausedState()
                    check(second.optLong("cachedPositionMs", -1) == seed.getLong("secondMs")) { "B cache changed after opening A: $second" }
                    check(second.optLong("positionMs") in (seed.getLong("secondMs") - 400)..(seed.getLong("secondMs") + 1800))
                    check(kotlin.math.abs(second.optLong("cachedProbePositionMs") - first.optLong("positionMs")) <= 400) { "Navigation wrote wrong CID: $second" }
                    steps += "new_process_restored_actual_mediakit_B_and_navigation_scoped_A=$second"
                }
                "badvalues" -> {
                    openOffline("A", firstCid)
                    val first = pausedState(secondCid)
                    check(first.isNull("cachedPositionMs") && first.isNull("cachedProbePositionMs")) { "Wrong type or negative progress accepted: $first" }
                    check(first.optLong("positionMs") in 0..1800) { "Bad typed cache did not fall back to beginning: $first" }
                    steps += "actual_Box_int_wrong_type_safe_read_and_decoder_fallback=$first"
                    click("返回主页"); openOffline("B", secondCid)
                    val second = pausedState()
                    check(second.isNull("cachedPositionMs") && second.optLong("positionMs") in 0..1800) { "Negative cache did not fall back: $second" }
                    steps += "negative_Hive_progress_safe_read_and_decoder_fallback=$second"
                }
                "catalog" -> {
                    openOffline("A", firstCid)
                    pausedState()
                    val catalog = state("downloadCacheState")
                    fun ids(name: String): Set<Long> {
                        val list = catalog.getJSONArray(name)
                        return (0 until list.length()).map { list.getLong(it) }.toSet()
                    }
                    val expected = setOf(firstCid, secondCid, 9000016L, 9000018L, 9000019L)
                    check(ids("completed") == expected) { "Broken records advertised playable or valid formats rejected: $catalog" }
                    check(ids("waiting") == setOf(9000015L)) { "Unfinished media escaped waiting queue: $catalog" }
                    steps += "real_download_service_isolates_bad_json_metadata_missing_empty_media_and_unsafe_tag=$catalog"
                    steps += "valid_mp4_dash_optional_required_audio_and_pgc_metadata_remain_supported"
                    steps += "is_completed_false_with_full_media_never_enters_playable_catalog"
                }
                else -> error("Unknown local recovery phase $phase")
            }
            capture("cache-recovery-$phase.png")
            passed = true
        } catch (t: Throwable) {
            result.putString("error", "${t.javaClass.simpleName}: ${t.message}")
            android.util.Log.e("CinemaCacheRecoveryRegression", "phase=$phase failed", t)
            try { capture("cache-recovery-failure.png") } catch (_: Throwable) { }
        } finally {
            result.putString("phase", phase)
            result.putString("pid", android.os.Process.myPid().toString())
            result.putString("passed", passed.toString())
            result.putString("steps", steps.joinToString("\n"))
            finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
        }
    }
}
