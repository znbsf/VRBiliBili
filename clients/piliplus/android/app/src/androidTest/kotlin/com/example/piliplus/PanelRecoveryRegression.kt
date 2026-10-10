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

/** Real Quest ordinary-panel playback/restart. Owned new fixtures only;
 * preserves all existing app data, accounts, preferences and progress.
 * Programmatic real callbacks do not establish physical controller comfort. */
class PanelRecoveryRegression : Instrumentation() {
    private var firstCid = 910041000007L
    private var secondCid = 910041000008L
    private var fixtureGroup = "4"
    @Volatile private var mainLifecycle = "not_started"
    private val steps = java.util.Collections.synchronizedList(mutableListOf<String>())
    private var mainActivity: Activity? = null

    private var phase = "invalid"
    override fun onCreate(arguments: Bundle?) {
        phase = arguments?.getString("phase") ?: error("Explicit phase required")
        val requestedCid = arguments.getString("firstCid")?.toLongOrNull()
            ?: error("Explicit valid new CID pair required")
        requestedCid.let { cid ->
            check(cid in 910041000009L..910041000099L && cid % 2L == 1L) { "New owned CID pair required" }
            firstCid = cid; secondCid = cid + 1L
            fixtureGroup = arguments.getString("fixtureGroup") ?: error("Unique fixture group required")
            check(fixtureGroup.matches(Regex("[0-9]{1,3}")))
        }
        super.onCreate(arguments); start()
    }
    override fun callActivityOnResume(activity: Activity) {
        super.callActivityOnResume(activity)
        if (activity is MainActivity) { mainLifecycle = "resumed"; checkpoint("main_resumed") }
    }
    override fun callActivityOnPause(activity: Activity) {
        if (activity is MainActivity) { mainLifecycle = "paused"; checkpoint("main_paused") }
        super.callActivityOnPause(activity)
    }
    private fun checkpoint(label: String) {
        val power = targetContext.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        steps += "checkpoint=" + JSONObject().put("label",label).put("uptimeMs",SystemClock.uptimeMillis())
            .put("interactive",power.isInteractive).put("mainLifecycle",mainLifecycle)
            .put("mainWindowFocused",mainActivity?.hasWindowFocus() ?: false)
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
            .put("total_bytes", media.length()).put("downloaded_bytes", media.length()).put("title", "VRBiliBili 自有恢复测试 $fixtureGroup $suffix")
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
        checkpoint("before_UI_$label")
        check(awaitNode(label).performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Click rejected: $label" }
        SystemClock.sleep(300)
        checkpoint("after_UI_$label")
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
    private fun openOffline(suffix: String,cid: Long,expectedStartMs: Long? = null): JSONObject {
        click("我的");click("离线缓存");click("VRBiliBili 自有恢复测试 $fixtureGroup $suffix")
        val loaded=awaitState("true Quest loaded owned fixture $suffix") {
            it.optLong("cid")==cid && it.optString("dataStatus")=="loaded" &&
                !it.optBoolean("processing") && it.optInt("width")>0 && it.optLong("durationMs")>0
        }
        steps += "actual_Quest_loaded_$suffix=$loaded"
        val loadedAt=SystemClock.uptimeMillis()
        if(expectedStartMs!=null) {
            // playerInit deliberately clears the one-shot defaultST after
            // passing it to Media.start. Null here is valid; decoder position
            // in the first 3.5 seconds below proves the actual seek instead.
            if(!loaded.isNull("restoreStartMs")) check(kotlin.math.abs(loaded.optLong("restoreStartMs")-expectedStartMs)<=400) { "Wrong source restore start: $loaded" }
            check(kotlin.math.abs(loaded.optLong("cachedPositionMs",-1)-expectedStartMs)<=400) { "Wrong persisted CID progress: $loaded" }
        }
        checkpoint("loaded_$suffix")
        // Loading and playback are separate acceptance facts. A normal real Play
        // button attempts recovery of an observed paused panel. The system and
        // lifecycle checkpoints establish whether off-headset sleep overlapped;
        // this action alone does not identify the Dart pause caller.
        SystemClock.sleep(500)
        val initial=state()
        steps += "autoplay_observed_$suffix=${initial.optBoolean("rawPlaying")}; positionMs=${initial.optLong("positionMs")}; no autoplay pass inferred from loaded state"
        if(!initial.optBoolean("rawPlaying")) {
            val play=awaitNode("播放 / 暂停")
            if(!state().optBoolean("rawPlaying")) {
                check(play.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Real Play button rejected" }
                steps += "actual_panel_Play_button_$suffix"
                checkpoint("real_Play_button_$suffix")
            }
        }
        val decoded=awaitState("true Quest MediaKit owned fixture $suffix") {
            it.optLong("cid")==cid && it.optString("dataStatus")=="loaded" &&
                !it.optBoolean("processing") && it.optBoolean("rawPlaying") && it.optInt("width")==320 && it.optInt("height")==180 && it.optLong("positionMs")>400 &&
                (expectedStartMs==null || (SystemClock.uptimeMillis()-loadedAt<=3500 && it.optLong("positionMs") in (expectedStartMs-400)..(expectedStartMs+3500)))
        }
        steps += "actual_Quest_MediaKit_decode_$suffix=$decoded"
        if(expectedStartMs!=null) steps += "actual_decoder_restored_start_$suffix; expectedMs=$expectedStartMs; observedMs=${decoded.optLong("positionMs")}; sinceLoadedMs=${SystemClock.uptimeMillis()-loadedAt}"
        return decoded
    }
    private fun labels(node: AccessibilityNodeInfo?,result: MutableList<String>) {
        if(node==null || result.size>=45)return
        val text=node.contentDescription?.toString() ?: node.text?.toString()
        if(!text.isNullOrEmpty())result+=text.take(140)
        for(i in 0 until node.childCount)labels(node.getChild(i),result)
    }
    private fun pausePanel(): JSONObject {
        if(state().optBoolean("rawPlaying")) {
            val button=awaitNode("播放 / 暂停")
            if(state().optBoolean("rawPlaying")) {
                check(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                steps += "actual_panel_Pause_button"
            }
        }
        val paused=awaitState("actual panel paused") { !it.optBoolean("rawPlaying") && !it.optBoolean("playing") }
        SystemClock.sleep(400)
        val stable=state()
        check(!stable.optBoolean("rawPlaying") && kotlin.math.abs(stable.optLong("positionMs")-paused.optLong("positionMs"))<=200)
        return stable
    }
    private fun advancePanel(times: Int) {
        val before=state()
        repeat(times) { click("前进 10 秒") }
        val after=awaitState("real panel forward callback") { it.optLong("cid")==before.optLong("cid") && it.optLong("positionMs")>=before.optLong("positionMs")+times*10000-2000 }
        steps += "actual_panel_forward_${times}_buttons=$after"
    }
    private fun panelSeed(seedFile: File) {
        openOffline("A",firstCid)
        check(state().isNull("cachedPositionMs")) { "Panel fixture already had progress" }
        advancePanel(1)
        val first=pausePanel()
        check(first.optLong("positionMs")>=400) { "First fixture did not actually progress" }
        click("返回主页")
        openOffline("B",secondCid)
        check(state().isNull("cachedPositionMs")) { "Second panel fixture already had progress" }
        val before=state()
        check(kotlin.math.abs(before.optLong("cachedProbePositionMs")-first.optLong("positionMs"))<=400) { "A not persisted on normal route exit" }
        advancePanel(2)
        val second=pausePanel()
        check(kotlin.math.abs(second.optLong("cachedProbePositionMs")-first.optLong("positionMs"))<=400) { "B overwrote A" }
        check(kotlin.math.abs(second.optLong("positionMs")-first.optLong("positionMs"))>8000)
        click("返回主页")
        // The video page owns the debug channel and clears it on disposal.
        // Observe normal navigation here; the fresh-process phase verifies the
        // actual saved values, instead of invoking a disposed page's handler.
        awaitNode("我的")
        seedFile.writeText(JSONObject().put("firstCid",firstCid).put("secondCid",secondCid)
            .put("firstMs",first.optLong("positionMs")).put("secondMs",second.optLong("positionMs"))
            .put("pid",android.os.Process.myPid()).put("coverage","real Quest ordinary panel").toString())
        steps += "real_Quest_panel_multi_CID_isolation=$second"
    }
    private fun panelRestart(seedFile: File) {
        val expected=JSONObject(seedFile.readText())
        check(expected.getInt("pid")!=android.os.Process.myPid()) { "New process required" }
        check(expected.getLong("firstCid")==firstCid && expected.getLong("secondCid")==secondCid)
        val firstStart=SystemClock.uptimeMillis();openOffline("A",firstCid,expected.getLong("firstMs"))
        pausePanel();val first=state(probeCid=secondCid)
        check(kotlin.math.abs(first.optLong("cachedPositionMs")-expected.getLong("firstMs"))<=400)
        check(kotlin.math.abs(first.optLong("cachedProbePositionMs")-expected.getLong("secondMs"))<=400)
        val firstAllowance=((SystemClock.uptimeMillis()-firstStart)*first.optDouble("playbackRate",1.0)).toLong()+1500
        check(first.optLong("positionMs") in (expected.getLong("firstMs")-400)..(expected.getLong("firstMs")+firstAllowance))
        steps += "real_Quest_panel_new_process_restored_A=$first"
        click("返回主页");val secondStart=SystemClock.uptimeMillis();openOffline("B",secondCid,expected.getLong("secondMs"))
        val second=pausePanel()
        check(kotlin.math.abs(second.optLong("cachedPositionMs")-expected.getLong("secondMs"))<=400)
        check(kotlin.math.abs(second.optLong("cachedProbePositionMs")-first.optLong("positionMs"))<=400)
        val secondAllowance=((SystemClock.uptimeMillis()-secondStart)*second.optDouble("playbackRate",1.0)).toLong()+1500
        check(second.optLong("positionMs") in (expected.getLong("secondMs")-400)..(expected.getLong("secondMs")+secondAllowance))
        steps += "real_Quest_panel_new_process_restored_B_and_preserved_A=$second"
        click("返回主页")
    }
    override fun onStart() {
        val result=Bundle();var passed=false
        try {
            check(android.os.Build.MODEL.contains("Quest")) { "Physical Quest required" }
            val info=targetContext.packageManager.getPackageInfo(targetContext.packageName,0)
            check(info.versionName=="0.1.0" && info.longVersionCode==6L) { "Wrong candidate version" }
            mainActivity=startActivitySync(Intent(targetContext,MainActivity::class.java).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val seedFile=File(targetContext.filesDir,"panel-recovery-seed-$fixtureGroup.json")
            when(phase) {
                "prepare" -> {
                    check(!seedFile.exists()) { "Seed record already exists" }
                    val deadline=SystemClock.uptimeMillis()+20000
                    var preflight=JSONObject()
                    while(SystemClock.uptimeMillis()<deadline) {
                        try { preflight=state("probeCacheKeys");if(preflight.optBoolean("ready"))break } catch(_:Throwable) { }
                        SystemClock.sleep(200)
                    }
                    check(preflight.optBoolean("ready") && preflight.getJSONArray("occupied").length()==0) { "CID occupied/unavailable; refusing writes" }
                    for(cid in listOf(firstCid,secondCid)) check(!File(checkNotNull(targetContext.getExternalFilesDir(null)), "download/vrbilibili_owned_recovery_$cid").exists()) { "Fixture path occupied" }
                    fixture(firstCid,"A");fixture(secondCid,"B")
                }
                "panelSeed" -> { check(!seedFile.exists());panelSeed(seedFile) }
                "panelRestart" -> panelRestart(seedFile)
                "lifecycle" -> {
                    openOffline("A",firstCid);val paused=pausePanel()
                    main { checkNotNull(mainActivity).moveTaskToBack(true) }
                    SystemClock.sleep(1000)
                    check(mainLifecycle=="paused") { "Background transition not observed" }
                    mainActivity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    val restored=awaitState("paused panel resumes") { !it.optBoolean("rawPlaying") && it.optLong("cid")==firstCid && kotlin.math.abs(it.optLong("positionMs")-paused.optLong("positionMs"))<=400 }
                    check(mainLifecycle=="resumed")
                    steps += "background_resume_paused=$restored; natural headset sleep not covered"
                    click("返回主页")
                }
                else -> error("Unsupported panel phase")
            }
            passed=true
        } catch(t:Throwable) {
            result.putString("error","${t.javaClass.simpleName}: ${t.message}")
            checkpoint("failure")
        } finally {
            result.putString("phase",phase);result.putString("pid",android.os.Process.myPid().toString())
            result.putString("passed",passed.toString());result.putString("steps",synchronized(steps) { steps.joinToString("\n") })
            finish(if(passed) Activity.RESULT_OK else Activity.RESULT_CANCELED,result)
        }
    }
}
