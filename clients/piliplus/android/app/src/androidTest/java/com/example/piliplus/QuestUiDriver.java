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
            targetActivity = startActivitySync(new Intent(getTargetContext(), MainActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AccessibilityNodeInfo card = await("", true, 35000);
            if (card == null) throw new Exception("No video card");
            capture("quest-home-ui.png");
            card.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            for (String label : new String[]{"返回上页", "返回主页", "播放 / 暂停", "放大画面", "播放设置"}) {
                AccessibilityNodeInfo n = await(label, false, 12000);
                if (n == null) throw new Exception("Missing control " + label);
                android.graphics.Rect b = new android.graphics.Rect(); n.getBoundsInScreen(b);
                if (b.height() < 58) throw new Exception("Small control " + label + b);
                steps.add(label + b);
            }
            if (find(getUiAutomation().getRootInActiveWindow(), "空间播放", false) != null) throw new Exception("XR entry still exists");
            click("播放 / 暂停");
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
            click("放大画面"); capture("quest-wide-ui.png");
            click("显示详情"); click("播放设置"); capture("quest-settings-ui.png"); click("关闭设置");
            click("返回上页"); if (await("", true, 12000) == null) throw new Exception("Back failed");
            await("", true, 12000).performAction(AccessibilityNodeInfo.ACTION_CLICK); click("返回主页");
            if (await("", true, 12000) == null) throw new Exception("Home failed");
            steps.add("back_and_home_passed"); passed = true;
        } catch (Exception e) {
            result.putString("error", e.toString());
            List<String> labels = new ArrayList<>(); collect(getUiAutomation().getRootInActiveWindow(), labels);
            result.putString("visible_labels", labels.toString());
        } finally {
            result.putString("steps", steps.toString()); result.putBoolean("passed", passed);
            finish(passed ? -1 : 0, result);
        }
    }
}
