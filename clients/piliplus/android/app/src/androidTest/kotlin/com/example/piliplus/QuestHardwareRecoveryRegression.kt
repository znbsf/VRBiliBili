package com.example.piliplus

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real Quest offline playback/immersive handoff/restart. Owned new fixtures only;
 * preserves all existing app data, accounts, preferences and progress.
 * Programmatic real callbacks do not establish physical controller comfort. */
class QuestHardwareRecoveryRegression : Instrumentation() {
    private val firstCid = 910041000007L
    private val secondCid = 910041000008L
    private val steps = mutableListOf<String>()
    private var cinema: CinemaActivity? = null
    private var mainActivity: Activity? = null

    private var phase = "seed"
    override fun onCreate(arguments: Bundle?) {
        phase = arguments?.getString("phase") ?: "seed"
        super.onCreate(arguments); start()
    }
    override fun callActivityOnResume(activity: Activity) {
        super.callActivityOnResume(activity)
        if (activity is CinemaActivity) cinema = activity
    }
    private fun <T> main(block: () -> T): T {
        val value = AtomicReference<T>(); val error = AtomicReference<Throwable>()
        runOnMainSync { try { value.set(block()) } catch (t: Throwable) { error.set(t) } }
        error.get()?.let { throw it }; return value.get()
    }
    private fun fixture(cid: Long, suffix: String) {
        val folder = File(checkNotNull(targetContext.getExternalFilesDir(null)), "download/vrbilibili_owned_recovery_$cid/c_$cid")
        check(!folder.exists()) { "Owned fixture path already occupied; refusing overwrite: $folder" }
        val media = File(folder, "80/0.mp4"); check(media.parentFile!!.mkdirs() || media.parentFile!!.isDirectory)
        context.assets.open("quest-recovery-owned-60s.mp4").use { input -> media.outputStream().use { input.copyTo(it) } }
        val page = JSONObject().put("cid", cid).put("page", 1).put("part", "回归片段$suffix")
            .put("has_alias", false).put("tid", 0).put("width", 320).put("height", 180).put("rotate", 0)
        val entry = JSONObject().put("media_type", 1).put("has_dash_audio", false).put("is_completed", true)
            .put("total_bytes", media.length()).put("downloaded_bytes", media.length()).put("title", "VRBiliBili 自有恢复测试 4 $suffix")
            .put("type_tag", "80").put("cover", "").put("video_quality", 80).put("prefered_video_quality", 80)
            .put("quality_pithy_description", "原创离线fixture").put("guessed_total_bytes", media.length())
            .put("total_time_milli", 60000).put("danmaku_count", 0).put("time_update_stamp", cid)
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
        val power=targetContext.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if(!power.isInteractive) {
            uiAutomation.executeShellCommand("input keyevent 224").close()
            steps += "normal_wake_key_for_UI_action_$label; no system settings/sensor override"
            SystemClock.sleep(300)
        }
        val end = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < end) {
            val root = uiAutomation.rootInActiveWindow
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
            checkNotNull(MainActivity.debugChannel).invokeMethod(method, mapOf("probeCid" to probeCid, "probeCids" to listOf(firstCid,secondCid)), object : MethodChannel.Result {
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
    private fun nativeState(): JSONObject {
        val file = File(targetContext.filesDir, "cinema-status.json")
        return if (file.isFile) try { JSONObject(file.readText()) } catch (_: Exception) { JSONObject() } else JSONObject()
    }
    private fun command(name: String) {
        main { checkNotNull(CinemaSession.testCommand) { "True Quest cinema command not ready" }.invoke(name) }
        SystemClock.sleep(200)
    }
    private fun awaitNative(label: String, predicate: (JSONObject) -> Boolean): JSONObject {
        val end=SystemClock.uptimeMillis()+35000
        var last=JSONObject()
        while (SystemClock.uptimeMillis()<end) {
            last=nativeState();if(predicate(last)) return last
            SystemClock.sleep(150)
        }
        error("Native timeout or headset focus/tracking unavailable: $label: $last")
    }
    private fun capture(name: String) {
        try {
            val target=cinema ?: mainActivity
            val bitmap=checkNotNull(uiAutomation.takeScreenshot(checkNotNull(target).window))
            File(targetContext.filesDir,name).outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
            bitmap.recycle()
            steps += "actual_Activity_window_capture_$name"
        } catch(_:Throwable) {
            steps += "Activity_window_capture_unavailable_$name; screenshot and XR compositor view not claimed"
        }
    }
    private fun openOffline(suffix: String,cid: Long): JSONObject {
        click("我的");click("离线缓存");click("VRBiliBili 自有恢复测试 4 $suffix")
        val decoded=awaitState("true Quest MediaKit owned fixture $suffix") {
            it.optLong("cid")==cid && it.optString("dataStatus")=="loaded" &&
                !it.optBoolean("processing") && it.optBoolean("rawPlaying") && it.optInt("width")>0 && it.optLong("positionMs")>400
        }
        steps += "actual_Quest_MediaKit_decode_$suffix=$decoded"
        return decoded
    }
    private fun enter(cid: Long): JSONObject {
        val oldSession=nativeState().optString("sessionId")
        val opened=SystemClock.uptimeMillis()
        cinema=null;click("全屏影院")
        val decoded=awaitNative("real CinemaActivity and frames") {
            cinema!=null && CinemaSession.media?.cid==cid && it.optLong("cid")==cid &&
                it.optString("sessionId").isNotEmpty() && it.optString("sessionId")!=oldSession &&
                it.optLong("sampleUptimeMs")>=opened && it.optBoolean("sceneReady") &&
                it.optBoolean("firstFrame") && it.optInt("videoFrames")>=12 && it.optInt("audioBuffers")>=3 && it.optBoolean("playing")
        }
        val flutter=state()
        check(flutter.optBoolean("cinemaActive") && !flutter.optBoolean("rawPlaying")) { "Two players active: $flutter" }
        steps += "true_Quest_immersive_decode_$cid=$decoded"
        return decoded
    }
    private fun closeNative(cid: Long): JSONObject {
        command("show")
        val before=nativeState()
        val paused=if(before.optBoolean("playing")) {
            command("tap:暂停")
            awaitNative("real native pause callback") { !it.optBoolean("playing") && it.optLong("positionMs")>1000 }.also {
                steps += "actual_native_pause_button_callback_$cid"
            }
        } else {
            check(before.optLong("positionMs")>=1000)
            steps += "native_already_paused_with_unattended_focus_$cid"
            before
        }
        SystemClock.sleep(650)
        val stable=nativeState()
        check(!stable.optBoolean("playing") && kotlin.math.abs(stable.optLong("positionMs")-paused.optLong("positionMs"))<=200) { "Native pause drifted: $stable" }
        val position=stable.optLong("positionMs")
        check(position<55000) { "Long fixture reached end before return" }
        try { capture("hardware-q4-native-$cid.png") } catch(_:Throwable) { steps += "XR_Activity_window_capture_unavailable_$cid; compositor and physical view not claimed" }
        command("tap:退出全屏")
        val returned=awaitState("true Quest cinema exit/restored paused Flutter") {
            !it.optBoolean("cinemaActive") && !it.optBoolean("processing") && it.optString("dataStatus")=="loaded" &&
                it.optLong("cid")==cid && !it.optBoolean("rawPlaying") && !it.optBoolean("playing") &&
                kotlin.math.abs(it.optLong("positionMs")-position)<=400 &&
                kotlin.math.abs(it.optLong("cachedPositionMs",-1)-position)<=400
        }
        SystemClock.sleep(650)
        val later=state()
        check(!later.optBoolean("rawPlaying") && kotlin.math.abs(later.optLong("positionMs")-returned.optLong("positionMs"))<=200)
        check(main { CinemaSession.media==null && CinemaSession.testCommand==null }) { "Native session was not drained" }
        awaitNode("全屏影院")
        steps += "true_Quest_return_paused_and_persisted_$cid=$returned"
        return returned
    }
    private fun labels(node: AccessibilityNodeInfo?,result: MutableList<String>) {
        if(node==null || result.size>=45)return
        val text=node.contentDescription?.toString() ?: node.text?.toString()
        if(!text.isNullOrEmpty())result+=text.take(140)
        for(i in 0 until node.childCount)labels(node.getChild(i),result)
    }
    override fun onStart() {
        val result=Bundle();var passed=false
        try {
            check(android.os.Build.MODEL.contains("Quest") && !android.os.Build.MODEL.contains("Spatial Simulator")) { "Physical Quest required" }
            val info=targetContext.packageManager.getPackageInfo(targetContext.packageName,0)
            check(info.versionName=="2.1.5-quest.20261004.4" && info.longVersionCode==2026100404L) { "Wrong installed candidate version" }
            steps += "installed_version=${info.versionName} code=${info.longVersionCode}"
            mainActivity=startActivitySync(Intent(targetContext,MainActivity::class.java).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val seedFile=File(targetContext.filesDir,"hardware-recovery-seed.json")
            if(phase=="prepare") {
                val deadline=SystemClock.uptimeMillis()+20000
                var preflight=JSONObject()
                while(SystemClock.uptimeMillis()<deadline) {
                    try { preflight=state("probeCacheKeys");if(preflight.optBoolean("ready"))break } catch(_:Throwable) { }
                    SystemClock.sleep(200)
                }
                check(preflight.optBoolean("ready") && preflight.getJSONArray("occupied").length()==0) { "Test CID keys occupied/unavailable; refusing all fixture/navigation writes: $preflight" }
                fixture(firstCid,"A");fixture(secondCid,"B")
                steps += "owned_fixture_preflight_and_new_files_only"
            } else if(phase=="seed") {
                val build=openOffline("A",firstCid)
                check(build.optString("buildVersionName")==info.versionName && build.optLong("buildVersionCode")==info.longVersionCode)
                check(build.optString("buildBaseCommit")=="cc7c8734bbcf7f0b50a192c58eaf6996e15e7a2d" && build.optString("buildLocalPatch").matches(Regex("[0-9a-f]{64}")))
                check(state().isNull("cachedPositionMs")) { "Test CID already had progress; refusing overwrite" }
                capture("hardware-q4-owned-player.png")
                enter(firstCid);awaitNative("first native advances") { it.optLong("positionMs")>=2000 || (!it.optBoolean("playing") && it.optLong("positionMs")>=1000) }
                val first=closeNative(firstCid)
                cinema=null;capture("hardware-q4-owned-return.png")
                click("返回主页");openOffline("B",secondCid)
                check(state().isNull("cachedPositionMs")) { "Second test CID already had progress" }
                val firstBaseline=state().optLong("cachedProbePositionMs")
                check(kotlin.math.abs(firstBaseline-first.optLong("cachedPositionMs"))<=400)
                val secondDecoded=enter(secondCid)
                command("show");command("tap:前进 10 秒")
                val secondSeek=awaitNative("actual second CID native seek callback") {
                    it.optLong("cid")==secondCid && it.optString("sessionId")==secondDecoded.optString("sessionId") &&
                        it.optLong("positionMs")>=secondDecoded.optLong("positionMs")+8000
                }
                steps += "actual_second_CID_native_seek_callback=$secondSeek"
                val second=closeNative(secondCid)
                check(second.optLong("cachedProbePositionMs")==firstBaseline) { "Second CID overwrote first" }
                check(kotlin.math.abs(firstBaseline-second.optLong("cachedPositionMs"))>800)
                seedFile.writeText(JSONObject().put("firstCid",firstCid).put("secondCid",secondCid)
                    .put("firstMs",firstBaseline).put("secondMs",second.optLong("cachedPositionMs"))
                    .put("pid",android.os.Process.myPid()).toString())
                steps += "true_Quest_multi_CID_isolation_and_seed_saved"
            } else if(phase=="restart") {
                val expected=JSONObject(seedFile.readText())
                check(expected.getInt("pid")!=android.os.Process.myPid()) { "New process required" }
                val firstStart=SystemClock.uptimeMillis()
                openOffline("A",firstCid);state("pauseForCacheRecovery")
                val first=state(probeCid=secondCid)
                check(first.optLong("cachedPositionMs")==expected.getLong("firstMs") && first.optLong("cachedProbePositionMs")==expected.getLong("secondMs"))
                val firstAllowance=((SystemClock.uptimeMillis()-firstStart)*first.optDouble("playbackRate",1.0)).toLong()+1500
                check(first.optLong("positionMs") in (expected.getLong("firstMs")-400)..(expected.getLong("firstMs")+firstAllowance))
                steps += "true_Quest_new_process_restored_A=$first"
                click("返回主页");val secondStart=SystemClock.uptimeMillis()
                openOffline("B",secondCid);state("pauseForCacheRecovery")
                val second=state()
                check(second.optLong("cachedPositionMs")==expected.getLong("secondMs"))
                val secondAllowance=((SystemClock.uptimeMillis()-secondStart)*second.optDouble("playbackRate",1.0)).toLong()+1500
                check(second.optLong("positionMs") in (expected.getLong("secondMs")-400)..(expected.getLong("secondMs")+secondAllowance))
                check(kotlin.math.abs(second.optLong("cachedProbePositionMs")-first.optLong("positionMs"))<=400)
                steps += "true_Quest_new_process_restored_B_and_preserved_A=$second"
                capture("hardware-q4-restart-return.png")
            } else error("Unsupported hardware phase $phase")
            passed=true
        } catch(t:Throwable) {
            result.putString("error","${t.javaClass.simpleName}: ${t.message}")
            val visible=mutableListOf<String>();labels(uiAutomation.rootInActiveWindow,visible)
            result.putString("visible_labels",visible.joinToString(" | "))
            result.putString("native_state",nativeState().toString())
            android.util.Log.e("QuestHardwareRecoveryRegression","Hardware phase=$phase failed",t)
            try { capture("hardware-q4-failure.png") } catch(_:Throwable) { }
        } finally {
            // Close only a cinema session this runner owns; preserve unrelated apps.
            if(cinema!=null && CinemaSession.media!=null) try { command("finish");SystemClock.sleep(1500) } catch(_:Throwable) { }
            result.putString("phase",phase);result.putString("pid",android.os.Process.myPid().toString())
            result.putString("passed",passed.toString());result.putString("steps",steps.joinToString("\n"))
            finish(if(passed) Activity.RESULT_OK else Activity.RESULT_CANCELED,result)
        }
    }
}
