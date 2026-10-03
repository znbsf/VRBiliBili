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
    private void command(String name) {
        runOnMainSync(() -> {
            if (SpatialSession.INSTANCE.getTestCommand() == null) throw new IllegalStateException("No spatial session");
            SpatialSession.INSTANCE.getTestCommand().invoke(name);
        });
    }
    private JSONObject waitState(File file, java.util.function.Predicate<JSONObject> predicate, long timeout) throws Exception {
        long end = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < end) {
            wake();
            if (file.exists()) {
                try {
                    JSONObject state = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                    if (predicate.test(state)) return state;
                } catch (org.json.JSONException ignored) { }
            }
            SystemClock.sleep(300);
        }
        throw new Exception("Timed out waiting for " + file.getName());
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        boolean passed = false;
        try {
            File status = new File(getTargetContext().getFilesDir(), "spatial-status.json");
            if (status.exists()) status.delete();
            Intent intent = new Intent(getTargetContext(), MainActivity.class)
                .setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivitySync(intent);
            AccessibilityNodeInfo card = await("", true, 35000);
            if (card == null) throw new Exception("No real video card");
            steps.add("video_card=" + card.getContentDescription());
            if (!card.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new Exception("Card action rejected");
            steps.add("card_clicked");
            AccessibilityNodeInfo play = await("播放", false, 8000);
            if (play != null) { play.performAction(AccessibilityNodeInfo.ACTION_CLICK); steps.add("initial_play_clicked"); }
            AccessibilityNodeInfo spatial = await("空间播放", false, 25000);
            if (spatial == null) throw new Exception("Spatial playback button not found");
            if (!spatial.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new Exception("Spatial action rejected");
            steps.add("spatial_button_clicked");
            long end = SystemClock.uptimeMillis() + 30000;
            while (SystemClock.uptimeMillis() < end) {
                wake();
                if (status.exists()) {
                    String data = new String(Files.readAllBytes(status.toPath()), StandardCharsets.UTF_8);
                    JSONObject state = new JSONObject(data);
                    if (state.optInt("videoFrames") > 10 && state.optInt("audioBuffers") > 0) {
                        steps.add("native_playback=" + data); passed = true; break;
                    }
                }
                SystemClock.sleep(500);
            }
            if (!passed) throw new Exception("No native online video frames");
            passed = false;
            command("panels");
            JSONObject panels = waitState(status, state -> state.optInt("catalogCount") > 0 && state.optInt("qualityCount") > 0 && state.optInt("commentCharacters") > 30, 25000);
            steps.add("panels=" + panels);
            command("quality");
            JSONObject switched = waitState(status, state -> state.optInt("sourceChanges") > 0 && state.optBoolean("playing") && state.optInt("videoFrames") > 30, 35000);
            steps.add("quality_switched=" + switched);
            command("pause"); command("seek");
            waitState(status, state -> Math.abs(state.optLong("positionMs") - 3000) < 250 && !state.optBoolean("playing"), 8000);
            File handoff = new File(getTargetContext().getFilesDir(), "spatial-handoff.json");
            if (handoff.exists()) handoff.delete();
            command("finish");
            JSONObject returned = waitState(handoff, state -> Math.abs(state.optLong("positionMs") - 3000) < 600 && !state.optBoolean("playing"), 15000);
            if (returned.optLong("cid") != switched.optLong("cid")) throw new Exception("Returned to wrong video");
            steps.add("returned_paused=" + returned);
            passed = true;
        } catch (Exception e) {
            result.putString("error", e.getMessage());
            List<String> labels = new ArrayList<>(); collect(getUiAutomation().getRootInActiveWindow(), labels);
            result.putString("visible_labels", labels.toString());
        } finally {
            result.putString("steps", steps.toString());
            result.putBoolean("passed", passed);
            finish(passed ? -1 : 0, result);
        }
    }
}
