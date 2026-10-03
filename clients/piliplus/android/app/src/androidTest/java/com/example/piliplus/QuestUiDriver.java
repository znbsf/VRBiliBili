package com.example.piliplus;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** Runs actual Flutter accessibility actions, then observes native decoder counters. */
public class QuestUiDriver extends Instrumentation {
    private android.app.Activity targetActivity;
    private final List<String> steps = new ArrayList<>();
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private void wake() {
        try { getUiAutomation().executeShellCommand("input keyevent 224").close(); }
        catch (Exception ignored) { }
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n, String label, boolean card) {
        if (n == null) return null;
        String text = String.valueOf(n.getContentDescription()) + " " + n.getText();
        if (n.isClickable() && (card ? text.contains("UP：") : (label.equals("播放") ? text.trim().replace(" null", "").equals(label) : text.contains(label)))) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo result = find(n.getChild(i), label, card);
            if (result != null) return result;
        }
        return null;
    }
    private AccessibilityNodeInfo await(String label, boolean card, long timeout) {
        long end = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < end) {
            wake();
            AccessibilityNodeInfo result = find(getUiAutomation().getRootInActiveWindow(), label, card);
            if (result != null) return result;
            SystemClock.sleep(250);
        }
        return null;
    }
    private void collect(AccessibilityNodeInfo n, List<String> labels) {
        if (n == null || labels.size() > 45) return;
        String label = n.getContentDescription() != null ? n.getContentDescription().toString() :
            n.getText() != null ? n.getText().toString() : "";
        if (!label.isEmpty()) labels.add(label.substring(0, Math.min(120, label.length())));
        for (int i = 0; i < n.getChildCount(); i++) collect(n.getChild(i), labels);
    }
    private void capture(String name) throws Exception {
        android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot(targetActivity.getWindow());
        if (bitmap == null) throw new Exception("Screenshot unavailable");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new File(getTargetContext().getFilesDir(), name))) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
    private JSONObject cinemaState() throws Exception {
        File file = new File(getTargetContext().getFilesDir(), "cinema-status.json");
        if (!file.exists()) return new JSONObject();
        try { return new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)); }
        catch (Exception ignored) { return new JSONObject(); }
    }
    private void cinemaCommand(String command) {
        runOnMainSync(() -> { if (CinemaSession.INSTANCE.getTestCommand() != null) CinemaSession.INSTANCE.getTestCommand().invoke(command); });
        SystemClock.sleep(600);
    }
    private void captureCinema(String name) throws Exception {
        Files.write(new File(getTargetContext().getFilesDir(), "cinema-capture.txt").toPath(), name.getBytes(StandardCharsets.UTF_8));
        // The host captures the OpenXR compositor via metavr/metacam; Android screenshots omit it.
        for (int i = 0; i < 24; i++) { wake(); SystemClock.sleep(500); }
    }

    private JSONObject state() throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<JSONObject> value = new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(() -> MainActivity.Companion.getDebugChannel().invokeMethod("playerState", null, new io.flutter.plugin.common.MethodChannel.Result() {
            public void success(Object result) { if (result instanceof java.util.Map) value.set(new JSONObject((java.util.Map) result)); latch.countDown(); }
            public void error(String c, String m, Object d) { latch.countDown(); }
            public void notImplemented() { latch.countDown(); }
        }));
        if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS) || value.get() == null) throw new Exception("No player state");
        return value.get();
    }
    private void click(String label) throws Exception {
        AccessibilityNodeInfo n = await(label, false, 15000);
        if (n == null || !n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new Exception("Cannot click " + label);
        SystemClock.sleep(700);
    }
    @Override public void onStart() {
        Bundle result = new Bundle(); boolean passed = false;
        try {
            new File(getTargetContext().getFilesDir(), "cinema-status.json").delete();
            new File(getTargetContext().getFilesDir(), "cinema-capture.txt").delete();
            targetActivity = startActivitySync(new Intent(getTargetContext(), MainActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AccessibilityNodeInfo card = await("", true, 35000);
            if (card == null) throw new Exception("No video card");
            capture("quest-home-ui.png");
            card.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            for (String label : new String[]{"返回上页", "返回主页", "播放 / 暂停", "全屏影院", "选择画质", "播放设置"}) {
                AccessibilityNodeInfo n = await(label, false, 12000);
                if (n == null) throw new Exception("Missing control " + label);
                android.graphics.Rect b = new android.graphics.Rect(); n.getBoundsInScreen(b);
                if (b.height() < 58) throw new Exception("Small control " + label + b);
                steps.add(label + b);
            }
            if (find(getUiAutomation().getRootInActiveWindow(), "空间播放", false) != null) throw new Exception("XR entry still exists");
            // Selection itself must start playback; no second click is allowed here.
            long end = SystemClock.uptimeMillis() + 30000;
            JSONObject playing = state();
            while (SystemClock.uptimeMillis() < end && (!playing.optBoolean("playing") || playing.optInt("width") == 0 || playing.optLong("positionMs") < 1200)) {
                wake(); SystemClock.sleep(600); playing = state();
            }
            if (playing.optInt("width") == 0 || playing.optLong("positionMs") < 1200) throw new Exception("No decoded playback");
            steps.add("playing=" + playing);
            capture("quest-detail-ui.png");
            click("播放 / 暂停"); JSONObject paused = state(); SystemClock.sleep(1200); JSONObject still = state();
            if (still.optBoolean("playing") || Math.abs(still.optLong("positionMs") - paused.optLong("positionMs")) > 400) throw new Exception("Pause did not hold");
            click("前进 10 秒"); JSONObject seek = state();
            if (seek.optLong("positionMs") < paused.optLong("positionMs") + 8000) throw new Exception("Seek failed");
            steps.add("paused_seek=" + seek);
            click("选择画质"); capture("quest-quality-ui.png"); click("关闭画质选择");
            click("播放设置"); capture("quest-settings-ui.png"); click("关闭设置");
            click("关闭弹幕"); click("开启弹幕");
            click("全屏影院");
            JSONObject cinema = cinemaState();
            long cinemaEnd = SystemClock.uptimeMillis() + 45000;
            while (SystemClock.uptimeMillis() < cinemaEnd && (cinema.optInt("videoFrames") < 20 || cinema.optInt("audioBuffers") < 5)) {
                wake(); SystemClock.sleep(500); cinema = cinemaState();
            }
            if (cinema.optInt("videoFrames") < 20 || cinema.optInt("audioBuffers") < 5 || !cinema.optBoolean("sceneReady"))
                throw new Exception("Cinema decode failed: " + cinema);
            SystemClock.sleep(4500); cinema = cinemaState();
            if (cinema.optBoolean("controlsVisible") || cinema.optBoolean("passthrough")) throw new Exception("Cinema is not immersive: " + cinema);
            steps.add("cinema=" + cinema);
            cinemaCommand("quality"); long qualityEnd = SystemClock.uptimeMillis() + 30000;
            while (SystemClock.uptimeMillis() < qualityEnd && (cinemaState().optInt("sourceChanges") == 0 || !cinemaState().optBoolean("playing"))) {
                wake(); SystemClock.sleep(500);
            }
            if (cinemaState().optInt("sourceChanges") == 0 || !cinemaState().optBoolean("playing")) throw new Exception("Cinema quality switch failed: " + cinemaState());
            steps.add("cinema_quality=" + cinemaState());
            cinemaCommand("recenter"); cinemaCommand("cinema"); cinemaCommand("show"); captureCinema("quest-cinema-ui.png");
            cinemaCommand("pure"); SystemClock.sleep(4500); cinema = cinemaState();
            if (cinema.optBoolean("cinema") || cinema.optBoolean("controlsVisible")) throw new Exception("Pure picture failed: " + cinema);
            captureCinema("quest-pure-ui.png");
            cinemaCommand("show"); if (!cinemaState().optBoolean("controlsVisible")) throw new Exception("Cannot reveal controls");
            cinemaCommand("pause"); long cp = cinemaState().optLong("positionMs"); SystemClock.sleep(1000);
            if (cinemaState().optBoolean("playing") || Math.abs(cinemaState().optLong("positionMs") - cp) > 500) throw new Exception("Cinema pause failed");
            cinemaCommand("seek"); SystemClock.sleep(800); long sought = cinemaState().optLong("positionMs");
            if (sought < cp + 8000) throw new Exception("Cinema seek failed");
            cinemaCommand("cinema"); cinemaCommand("finish");
            if (await("全屏影院", false, 15000) == null) throw new Exception("Cinema return failed");
            JSONObject returned = state();
            if (returned.optBoolean("playing") || Math.abs(returned.optLong("positionMs") - sought) > 1200) throw new Exception("Handoff failed: " + returned);
            steps.add("cinema_pure_hide_show_seek_return_passed=" + returned);
            new File(getTargetContext().getFilesDir(), "cinema-status.json").delete();
            click("全屏影院"); long reenterEnd = SystemClock.uptimeMillis() + 20000;
            while (SystemClock.uptimeMillis() < reenterEnd && !cinemaState().optBoolean("firstFrame")) { wake(); SystemClock.sleep(500); }
            if (!cinemaState().optBoolean("firstFrame")) throw new Exception("Cinema reentry failed");
            cinemaCommand("finish");
            if (await("全屏影院", false, 15000) == null) throw new Exception("Second cinema return failed");
            steps.add("cinema_reentry_passed");
            click("返回上页"); if (await("", true, 12000) == null) throw new Exception("Back failed");
            await("", true, 12000).performAction(AccessibilityNodeInfo.ACTION_CLICK); click("返回主页");
            if (await("", true, 12000) == null) throw new Exception("Home failed");
            steps.add("back_and_home_passed"); passed = true;
        } catch (Exception e) {
            result.putString("error", e.toString());
            List<String> labels = new ArrayList<>(); collect(getUiAutomation().getRootInActiveWindow(), labels);
            result.putString("visible_labels", labels.toString());
        } finally {
            cinemaCommand("finish");
            result.putString("steps", steps.toString()); result.putBoolean("passed", passed);
            finish(passed ? -1 : 0, result);
        }
    }
}
