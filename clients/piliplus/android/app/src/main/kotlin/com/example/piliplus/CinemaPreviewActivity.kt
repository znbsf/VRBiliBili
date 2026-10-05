package com.example.piliplus

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource

/** x86 layout and playback preview. Does not claim to simulate the XR scene. */
class CinemaPreviewActivity : Activity() {
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var player: ExoPlayer
    private lateinit var canvas: FrameLayout
    private lateinit var hud: CinemaHud
    private var touch=0L
    private var scrubbing=false
    private var menuOpen=false
    private var environment="dark"
    private var screenSize=1f
    private lateinit var callbacks: CinemaCallbackGuard
    private var resumed=false
    private fun dp(v:Int)=(v*resources.displayMetrics.density+.5f).toInt()
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        if(!android.os.Build.MODEL.contains("Spatial Simulator") || CinemaSession.media==null) { finish();return }
        callbacks=CinemaCallbackGuard(CinemaSession.generation)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val holder=FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        canvas=FrameLayout(this)
        holder.addView(canvas,FrameLayout.LayoutParams(dp(1200),dp(675),Gravity.CENTER))
        holder.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ ->
            val scale=minOf(holder.width.toFloat()/dp(1200),holder.height.toFloat()/dp(675))
            canvas.scaleX=scale;canvas.scaleY=scale
        }
        setContentView(holder)
        player=ExoPlayer.Builder(this).build()
        val surface=SurfaceView(this)
        canvas.addView(surface,FrameLayout.LayoutParams(-1,-1))
        player.setVideoSurfaceView(surface)
        load(CinemaSession.media!!.positionMs)
        hud=CinemaHud(canvas,"模拟器控件预览（非 XR） · ${CinemaSession.title}", { action ->
            touch=SystemClock.uptimeMillis()
            when(action) {
                "play" -> if(player.isPlaying) player.pause() else player.play()
                "back" -> player.seekTo((player.currentPosition-10000).coerceAtLeast(0))
                "forward" -> player.seekTo((player.currentPosition+10000).coerceAtMost(player.duration.coerceAtLeast(0)))
                "comments" -> { hud.commentsButton?.let { it.active=!it.active;it.invalidate() } }
                "quality" -> {
                    val panel=menu("画质")
                    val catalogEpoch=callbacks.begin()
                    CinemaSession.request?.invoke("catalog",emptyMap()) catalog@{ response ->
                        if(!callbacks.accepts(catalogEpoch,CinemaSession.generation,isFinishing,isDestroyed) || panel.parent==null)return@catalog
                        @Suppress("UNCHECKED_CAST")
                        val items=response?.get("qualities") as? List<Map<String,Any>> ?: emptyList()
                        items.forEach { q -> CinemaMenu.item(panel,q["title"].toString(),q["selected"]==true) select@{
                            if(!callbacks.accepts(catalogEpoch,CinemaSession.generation,isFinishing,isDestroyed) || panel.parent==null)return@select
                            val wasPlaying=player.playWhenReady
                            val position=player.currentPosition;player.pause();closeMenu()
                            val qualityEpoch=callbacks.begin()
                            CinemaSession.request?.invoke("quality",mapOf("id" to q["id"]!!)) quality@{ data ->
                                if(!callbacks.accepts(qualityEpoch,CinemaSession.generation,isFinishing,isDestroyed))return@quality
                                val url=data?.get("video") as? String
                                if(url!=null) {
                                    @Suppress("UNCHECKED_CAST")
                                    val headers=data["headers"] as? Map<String,String> ?: emptyMap()
                                    CinemaSession.media=CinemaMedia(url,data["audio"] as? String,(data["cid"] as? Number)?.toLong() ?: 0,headers,position)
                                    load(position,wasPlaying && resumed)
                                }
                            }
                        } }
                    }
                }
                "environment" -> {
                    val panel=menu("观看环境 · 仅验证菜单")
                    listOf("dark" to "深色影院","light" to "浅色空间","pure" to "纯画面","passthrough" to "混合现实").forEach { (id,label) ->
                        CinemaMenu.item(panel,label,environment==id) { environment=id;closeMenu() }
                    }
                }
                "size" -> {
                    val panel=menu("屏幕大小 · 仅验证菜单")
                    listOf(.8f to "小",1f to "中",1.25f to "大").forEach { (size,label) -> CinemaMenu.item(panel,label,screenSize==size) { screenSize=size;closeMenu() } }
                }
                "center" -> Toast.makeText(this,"空间定位需在头显验证",Toast.LENGTH_SHORT).show()
                "exit" -> finish()
            }
        }, { value -> if(player.duration>0)player.seekTo(player.duration*value/1000) }, { scrubbing=it;show() })
        canvas.setOnClickListener { if(hud.hud?.visibility==View.VISIBLE && player.isPlaying) hide() else show() }
        show();handler.post(tick)
    }
    private fun menu(title:String):LinearLayout { menuOpen=true;show();return CinemaMenu.open(canvas,title) {closeMenu()} }
    private fun closeMenu() { canvas.findViewWithTag<View>("quality-menu")?.let {canvas.removeView(it)};menuOpen=false;show() }
    private fun show() { touch=SystemClock.uptimeMillis();hud.hud?.visibility=View.VISIBLE;hud.heading?.visibility=View.VISIBLE }
    private fun hide() { hud.hud?.visibility=View.GONE;hud.heading?.visibility=View.GONE }
    private fun load(position:Long,autoplay:Boolean=true) {
        val m=CinemaSession.media ?: return
        val factory=ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this,DefaultHttpDataSource.Factory().setDefaultRequestProperties(m.headers)))
        val video=factory.createMediaSource(MediaItem.fromUri(m.video))
        player.setMediaSource(if(m.audio.isNullOrBlank()) video else MergingMediaSource(video,factory.createMediaSource(MediaItem.fromUri(m.audio!!))))
        player.seekTo(position);player.playWhenReady=autoplay;player.prepare()
    }
    private val tick=object:Runnable { override fun run() {
        if(isFinishing)return
        hud.playingButton?.update(if(player.isPlaying)"pause" else "play",if(player.isPlaying)"暂停" else "播放")
        hud.status?.text="${player.currentPosition/1000}s / ${player.duration.coerceAtLeast(0)/1000}s"
        if(!scrubbing && player.duration>0)hud.progress?.progress=(player.currentPosition*1000/player.duration).toInt()
        if(player.isPlaying && !menuOpen && !scrubbing && SystemClock.uptimeMillis()-touch>3500)hide()
        filesDir.resolve("preview-status.json").writeText(org.json.JSONObject(mapOf(
            "simulatorPreview" to true, "positionMs" to player.currentPosition,
            "playing" to player.isPlaying, "width" to player.videoSize.width,
            "height" to player.videoSize.height, "controlsVisible" to (hud.hud?.visibility==View.VISIBLE)
        )).toString())
        handler.postDelayed(this,250)
    } }
    override fun finish() {
        if(::callbacks.isInitialized)callbacks.close()
        resumed=false
        super.finish()
    }
    override fun onResume() { super.onResume();resumed=true }
    override fun onPause() { resumed=false;if(::player.isInitialized)player.pause();super.onPause() }
    override fun onDestroy() {
        if(::callbacks.isInitialized)callbacks.close()
        handler.removeCallbacksAndMessages(null)
        val position=if(::player.isInitialized)player.currentPosition else 0
        if(::player.isInitialized)player.release()
        super.onDestroy();if(isFinishing)CinemaSession.complete(position)
    }
}
