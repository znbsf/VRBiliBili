package com.example.piliplus;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in release-signed test APK. Operates real controls of the unchanged app.
 * No account storage, URL logs, sensor override, or production test endpoint. */
public class PanelAcceptanceProbe extends Instrumentation {
    private Activity activity;
    private volatile boolean waking;
    private final JSONArray events = new JSONArray();
    private int delivered;
    private final JSONObject result = new JSONObject();
    private File output;
    private String label(AccessibilityNodeInfo n) {
        return String.valueOf(n.getContentDescription()) + " " + String.valueOf(n.getText());
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n, String text, boolean slider) {
        if (n == null) return null;
        if ("io.github.vrbilibili.quest".contentEquals(n.getPackageName() == null ? "" : n.getPackageName()) &&
            (slider ? "android.widget.SeekBar".contentEquals(n.getClassName()) : n.isClickable() && label(n).contains(text))) return n;
        for (int i=0;i<n.getChildCount();i++) { AccessibilityNodeInfo v=find(n.getChild(i),text,slider);if(v!=null)return v; }
        return null;
    }
    private AccessibilityNodeInfo await(String text, boolean slider) {
        long end=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<end) {
            AccessibilityNodeInfo n=find(getUiAutomation().getRootInActiveWindow(),text,slider);
            if(n!=null)return n;SystemClock.sleep(250);
        }
        throw new IllegalStateException("Missing app control: "+text);
    }
    private void click(String text) {
        if(!await(text,false).performAction(AccessibilityNodeInfo.ACTION_CLICK))throw new IllegalStateException("Rejected: "+text);
        SystemClock.sleep(600);
    }
    private String timeLabel(AccessibilityNodeInfo n) {
        if(n==null)return "";
        String s=label(n);if(s.contains("正在观看") && s.matches("(?s).*\\d+:\\d+ / \\d+:\\d+.*"))return s;
        for(int i=0;i<n.getChildCount();i++){String s2=timeLabel(n.getChild(i));if(!s2.isEmpty())return s2;}
        return "";
    }
    private int position() {
        String s=timeLabel(getUiAutomation().getRootInActiveWindow());
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+):(\\d+) / ").matcher(s);
        return m.find()?Integer.parseInt(m.group(1))*60+Integer.parseInt(m.group(2)):-1;
    }
    private JSONObject sample() throws Exception {
        return new JSONObject().put("uptimeMs",SystemClock.uptimeMillis()).put("positionSeconds",position())
            .put("timeLabel",timeLabel(getUiAutomation().getRootInActiveWindow()));
    }
    private void screenshot(String name) throws Exception {
        Bitmap b=getUiAutomation().takeScreenshot(activity.getWindow());
        if(b==null){result.put(name,"window screenshot unavailable");return;}
        File f=new File(output,name+".png");try(FileOutputStream stream=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,stream);}
        result.put(name,new JSONObject().put("width",b.getWidth()).put("height",b.getHeight()));b.recycle();
    }
    private void shell(String command) {
        try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);
            java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {byte[] b=new byte[1024];while(in.read(b)>0){} }
        catch(Exception e){throw new RuntimeException(e);}
    }
    private void mouse(float x,float y,int action,long down) {
        MotionEvent.PointerProperties props=new MotionEvent.PointerProperties();props.id=0;props.toolType=MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords coords=new MotionEvent.PointerCoords();coords.x=x;coords.y=y;coords.pressure=action==MotionEvent.ACTION_UP?0:1;coords.size=1;
        MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,
            new MotionEvent.PointerProperties[]{props},new MotionEvent.PointerCoords[]{coords},0,
            action==MotionEvent.ACTION_UP?0:MotionEvent.BUTTON_PRIMARY,1,1,0,0,InputDevice.SOURCE_MOUSE,0);
        if(!getUiAutomation().injectInputEvent(e,true))throw new IllegalStateException("Mouse injection rejected");e.recycle();
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle bundle=new Bundle();Window.Callback[] original=new Window.Callback[1];Thread wakeThread=null;
        try {
            output=new File(getTargetContext().getExternalFilesDir(null),"panel-acceptance-"+System.currentTimeMillis());
            if(!output.mkdirs())throw new IllegalStateException("Cannot create unique evidence directory");
            waking=true;wakeThread=new Thread(()->{while(waking){try{shell("input keyevent KEYCODE_WAKEUP");SystemClock.sleep(2000);}catch(Exception e){return;}}});wakeThread.start();
            activity=startActivitySync(new Intent().setClassName("io.github.vrbilibili.quest","com.example.piliplus.MainActivity").setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            SystemClock.sleep(2000);
            AccessibilityNodeInfo cancel=find(getUiAutomation().getRootInActiveWindow(),"取消",false);
            if(cancel!=null)cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            result.put("upstreamUpdateDialogObserved",cancel!=null);
            click("UP：");await("",true);SystemClock.sleep(2500);
            result.put("selected",sample());screenshot("video-panel");
            int p1=position();SystemClock.sleep(1700);int p2=position();
            if(p2>p1)click("播放 / 暂停");
            int paused=position();SystemClock.sleep(1400);
            result.put("pauseStable",position()==paused && paused>=0);
            click("展开大屏");screenshot("video-expanded");
            Rect slider=new Rect();await("",true).getBoundsInScreen(slider);
            final int width=activity.getWindow().getDecorView().getWidth();final float y=slider.exactCenterY();
            result.put("windowWidth",width).put("sliderSemanticBounds",slider.toShortString());
            runOnMainSync(()->{original[0]=activity.getWindow().getCallback();
                activity.getWindow().setCallback((Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class[]{Window.Callback.class},(proxy,method,args)->{
                    if(method.getName().equals("dispatchTouchEvent")) {MotionEvent e=(MotionEvent)args[0];delivered++;if(events.length()<40 || e.getActionMasked()!=MotionEvent.ACTION_MOVE)events.put(new JSONObject().put("route","system-window").put("action",e.getActionMasked()).put("x",e.getX()).put("y",e.getY()).put("source",e.getSource()).put("buttons",e.getButtonState()));}
                    try{return method.invoke(original[0],args);}catch(InvocationTargetException e){throw e.getCause();}
                }));});
            result.put("beforeMouseDrag",sample());
            shell("input mouse swipe "+(int)(width*.2)+" "+(int)y+" "+(int)(width*.45)+" "+(int)y+" 800");
            SystemClock.sleep(500);result.put("afterMouseDrag",sample());result.put("mouseDeliveredEvents",delivered);
            result.put("mouseEvents",new JSONArray(events.toString()));
            runOnMainSync(()->{while(events.length()>0)events.remove(events.length()-1);delivered=0;});
            long mouseDown=SystemClock.uptimeMillis();mouse(width*.2f,y,MotionEvent.ACTION_DOWN,mouseDown);
            for(int i=1;i<=12;i++){SystemClock.sleep(40);mouse(width*(.2f+.25f*i/12),y,MotionEvent.ACTION_MOVE,mouseDown);}
            result.put("beforePrimaryMouseUp",sample());mouse(width*.45f,y,MotionEvent.ACTION_UP,mouseDown);
            SystemClock.sleep(500);result.put("afterPrimaryMouseDrag",sample());result.put("primaryMouseEvents",new JSONArray(events.toString()));
            runOnMainSync(()->{while(events.length()>0)events.remove(events.length()-1);delivered=0;});
            result.put("beforeRawDrag",sample());
            shell("input touchscreen swipe "+(int)(width*.2)+" "+(int)y+" "+(int)(width*.65)+" "+(int)y+" 800");
            SystemClock.sleep(500);
            int observed=position();for(int retry=0;observed<0 && retry<8;retry++){SystemClock.sleep(300);observed=position();}
            result.put("afterRawDrag",sample());result.put("rawDeliveredEvents",delivered);
            screenshot("after-drag");
            result.put("events",events).put("rawDragChangedPosition",observed>paused+30);
            if(observed<=paused+30)throw new IllegalStateException("Real touchscreen drag did not seek");
            result.put("naturalSleepClaim",false);
        }catch(Throwable t){try{result.put("error",t.toString());}catch(Exception ignored){}}
        finally {
            waking=false;if(wakeThread!=null)try{wakeThread.join(3500);}catch(Exception ignored){}
            if(activity!=null&&original[0]!=null)runOnMainSync(()->activity.getWindow().setCallback(original[0]));
            bundle.putString("result",result.toString());
            try{File f=new File(output,"result.json");try(FileOutputStream stream=new FileOutputStream(f)){stream.write(result.toString(2).getBytes(StandardCharsets.UTF_8));}bundle.putString("evidenceDirectory",output.getAbsolutePath());bundle.putString("result",result.toString());}
            catch(Exception e){bundle.putString("saveError",e.toString());}
            finish(result.has("error")?0:-1,bundle);
        }
    }
}
