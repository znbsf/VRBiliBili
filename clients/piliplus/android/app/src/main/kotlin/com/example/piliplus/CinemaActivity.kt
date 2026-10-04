package com.example.piliplus

import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import com.meta.spatial.core.*
import com.meta.spatial.runtime.*
import com.meta.spatial.toolkit.*
import com.meta.spatial.vr.VRFeature
import java.util.concurrent.CompletableFuture
import org.json.JSONObject

/** A single fixed cinema screen. No floating catalogue or draggable window layout. */
class CinemaActivity : AppSystemActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var video: Entity? = null
    private var overlay: Entity? = null
    private var anchor = Pose()
    private val room = mutableListOf<Pair<Entity, Pose>>()
    private var hud: LinearLayout? = null
    private var root: FrameLayout? = null
    private var danmaku: CinemaOverlayView? = null
    private var status: TextView? = null
    private var heading: TextView? = null
    private var progress: SeekBar? = null
    private var playingButton: CinemaControl? = null
    private var roomButton: CinemaControl? = null
    private var qualityPanel: LinearLayout? = null
    private var dragging = false
    private var closing = false
    private var busy = false
    private var requestEpoch = 0
    private var firstFrame = false
    private var alignedAfterVideo = false
    private var sourceChanges = 0
    private var ratio = 16f / 9f
    private var lastPosition = 0L
    private var resumePlayback = false
    private var resumeAfterFocus = false
    private var resumed = false
    private var spatialFocused = true
    private var lastTouch = SystemClock.uptimeMillis()
    private var ticks = 0
    private var fetchingDanmaku = false
    private var cinema = true
    private var environment = "dark"
    private var screenScale = 1f
    private var floorMaterial: SceneMaterial? = null
    private var skyMaterial: SceneMaterial? = null
    private var error: String? = null
    private val media get() = CinemaSession.media
    private val accent = Color.rgb(251, 114, 153)

    override fun registerFeatures(): List<SpatialFeature> = listOf(VRFeature(this))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (media == null) { finish(); return }
        lastPosition = savedInstanceState?.getLong("position") ?: media!!.positionMs
        cinema = true
        screenScale = getSharedPreferences("cinema", MODE_PRIVATE).getFloat("size", 1f)
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            CinemaSession.testCommand = { command -> runOnUiThread {
                if (command.startsWith("tap:")) {
                    fun find(view: View, label: String): View? {
                        if (view.isClickable && view.contentDescription?.toString() == label) return view
                        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) {
                            find(view.getChildAt(i), label)?.let { return it }
                        }
                        return null
                    }
                    root?.let { find(it, command.removePrefix("tap:")) }?.performClick()
                    writeStatus()
                    return@runOnUiThread
                }
                when (command) {
                    "environment-menu" -> showEnvironment()
                    "size-menu" -> showSize()
                    "close-menu" -> closeMenu()
                    "large" -> { screenScale=1.25f;applyLayout() }
                    "medium" -> { screenScale=1f;applyLayout() }
                    "light" -> setEnvironment("light")
                    "passthrough" -> setEnvironment("passthrough")
                    "show" -> showControls()
                    "recenter" -> recenter()
                    "quality" -> CinemaSession.request?.invoke("catalog", emptyMap()) { data ->
                        @Suppress("UNCHECKED_CAST")
                        val options = data?.get("qualities") as? List<Map<String, Any>> ?: emptyList()
                        val id = options.lastOrNull { it["selected"] != true }?.get("id") as? Number
                        if (id != null) switchQuality(id)
                    }
                    "pure" -> setCinema(false)
                    "cinema" -> setCinema(true)
                    "pause" -> player?.pause()
                    "play" -> player?.play()
                    "seek" -> player?.seekTo((player?.currentPosition ?: 0) + 10000)
                    "finish" -> finish()
                }
                writeStatus()
            } }
        }
    }

    override fun onSceneReady() {
        super.onSceneReady()
        closing = false
        room.clear()
        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        scene.enablePassthrough(false)
        scene.setLightingEnvironment(ambientColor = Vector3(.22f),
            sunColor = Vector3(.12f), sunDirection = Vector3(0f, -1f, 1f))
        video = Entity.create(listOf(Panel(R.id.cinema_video), Transform()))
        overlay = Entity.create(listOf(Panel(R.id.cinema_overlay), Transform()))
        createRoom()
        recenter()
        applyLayout()
        handler.post(tick)
        handler.postDelayed({ if (!closing) recenter() }, 750)
    }

    override fun registerPanels(): List<PanelRegistration> = listOf(
        VideoSurfacePanelRegistration(R.id.cinema_video,
            surfaceConsumer = { _, surface -> runOnUiThread {
                if (!closing && media != null) {
                    if (player == null) createPlayer()
                    player?.setVideoSurface(surface)
                }
            } },
            settingsCreator = { MediaPanelSettings(
                shape = QuadShapeOptions(width = 4.8f, height = 2.7f),
                display = PixelDisplayOptions(width = 1920, height = 1080)) }),
        LayoutXMLPanelRegistration(R.id.cinema_overlay,
            layoutIdCreator = { R.layout.cinema_overlay },
            settingsCreator = { UIPanelSettings(
                shape = QuadShapeOptions(width = 4.8f, height = 2.7f),
                display = DpDisplayOptions(width = 1200f, height = 675f),
                style = PanelStyleOptions(themeResourceId = R.style.CinemaOverlayTheme)) },
            panelSetupWithRootView = { view, _, _ -> runOnUiThread {
                buildOverlay(view.findViewById(R.id.cinema_overlay_root))
            } })
    )

    private fun createPlayer() {
        player = ExoPlayer.Builder(this).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    firstFrame = true
                    if (!alignedAfterVideo) {
                        handler.postDelayed({ if (!closing) recenter() }, 500)
                    }
                }
                override fun onVideoSizeChanged(size: VideoSize) {
                    if (size.width > 0 && size.height > 0) {
                        ratio = size.width * size.pixelWidthHeightRatio / size.height
                        applyLayout()
                    }
                }
                override fun onPlayerError(e: PlaybackException) {
                    error = "播放失败 (${e.errorCode})，请返回播放页重试"
                    showControls()
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) showControls()
                }
            })
            loadMedia(p)
        }
    }

    private fun loadMedia(p: ExoPlayer, autoplay: Boolean = true) {
        val m = media ?: return
        val http = DefaultHttpDataSource.Factory().setDefaultRequestProperties(m.headers)
            .setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
        val factory = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this, http))
        val v = factory.createMediaSource(MediaItem.fromUri(m.video))
        val merged = if (m.audio.isNullOrBlank()) v else MergingMediaSource(v,
            factory.createMediaSource(MediaItem.fromUri(m.audio!!)))
        p.setMediaSource(merged); p.seekTo(lastPosition); p.prepare(); p.playWhenReady = autoplay
    }

    private fun dp(value: Int): Int = (value * (root?.resources?.displayMetrics?.density ?: resources.displayMetrics.density) + .5f).toInt()

    private fun buildOverlay(frame: FrameLayout) {
        root = frame
        frame.setOnClickListener {
            if (hud?.visibility == View.VISIBLE && player?.isPlaying == true) hideControls()
            else showControls()
        }
        danmaku = CinemaOverlayView(frame.context) { player?.currentPosition ?: lastPosition }
        danmaku?.visibility = if (environment == "pure") View.INVISIBLE else View.VISIBLE
        frame.addView(danmaku, FrameLayout.LayoutParams(-1, -1))
        lateinit var controls: CinemaHud
        controls= CinemaHud(frame, CinemaSession.title, { action ->
            lastTouch=SystemClock.uptimeMillis()
            when(action) {
                "back" -> player?.let { it.seekTo((it.currentPosition-10000).coerceAtLeast(0)) }
                "forward" -> player?.let { it.seekTo((it.currentPosition+10000).coerceAtMost(it.duration.coerceAtLeast(0))) }
                "play" -> player?.let { if(it.isPlaying) it.pause() else { if(it.playbackState==Player.STATE_ENDED) it.seekTo(0);it.play() } }
                "comments" -> danmaku?.let { it.danmakuEnabled=!it.danmakuEnabled; if(!it.danmakuEnabled)it.comments.clear();controls.commentsButton?.active=it.danmakuEnabled;controls.commentsButton?.invalidate() }
                "quality" -> showQuality()
                "environment" -> showEnvironment()
                "size" -> showSize()
                "center" -> recenter()
                "exit" -> finish()
            }
        }, { value -> player?.let { if(it.duration>0)it.seekTo(it.duration*value/1000) } }, { value -> dragging=value;showControls() })
        heading=controls.heading;hud=controls.hud;status=controls.status;progress=controls.progress
        playingButton=controls.playingButton;roomButton=controls.roomButton
        showControls()
    }

    private fun showControls() { heading?.visibility=View.VISIBLE; hud?.visibility = View.VISIBLE; lastTouch = SystemClock.uptimeMillis() }
    private fun hideControls() {
        heading?.visibility=View.GONE
        hud?.visibility = View.GONE
        root?.findViewWithTag<View>("quality-menu")?.let { root?.removeView(it) }
        qualityPanel = null
    }
    private fun menu(title: String): LinearLayout {
        showControls()
        return CinemaMenu.open(root!!,title) { closeMenu() }.also { qualityPanel=it }
    }
    private fun closeMenu() {
        root?.findViewWithTag<View>("quality-menu")?.let { root?.removeView(it) }
        qualityPanel=null; showControls()
    }
    private fun menuItem(panel: LinearLayout, label: String, selected: Boolean, action: () -> Unit) {
        CinemaMenu.item(panel,label,selected) { lastTouch=SystemClock.uptimeMillis();action() }
    }
    private fun showQuality() {
        val panel=menu("画质")
        CinemaSession.request?.invoke("catalog", emptyMap()) { response ->
            if(closing || qualityPanel !== panel) return@invoke
            @Suppress("UNCHECKED_CAST")
            val qualities=response?.get("qualities") as? List<Map<String,Any>> ?: emptyList()
            if(qualities.isEmpty()) menuItem(panel,"暂无可用画质",false) { closeMenu() }
            for(q in qualities) menuItem(panel,q["title"].toString(),q["selected"]==true) {
                closeMenu(); (q["id"] as? Number)?.let { switchQuality(it) }
            }
        }
    }
    private fun showEnvironment() {
        val panel=menu("观看环境")
        for((id,label) in listOf("dark" to "深色影院", "light" to "浅色空间", "pure" to "纯画面", "passthrough" to "混合现实"))
            menuItem(panel,label,environment==id) { setEnvironment(id);closeMenu() }
    }
    private fun showSize() {
        val panel=menu("屏幕大小")
        for((size,label) in listOf(.8f to "小",1f to "中",1.25f to "大"))
            menuItem(panel,label,screenScale==size) {
                screenScale=size; applyLayout()
                getSharedPreferences("cinema",MODE_PRIVATE).edit().putFloat("size",size).apply();closeMenu()
            }
    }

    private fun switchQuality(id: Number) {
        if (busy) return
        busy = true; error = null
        val epoch = ++requestEpoch
        val position = player?.currentPosition ?: lastPosition
        val wasPlaying = player?.playWhenReady == true
        player?.pause()
        handler.postDelayed({ if (busy && requestEpoch == epoch && !closing) {
            busy = false; requestEpoch++; error = "画质切换超时，请返回重试"; showControls()
        } }, 30000)
        CinemaSession.request?.invoke("quality", mapOf("id" to id.toInt())) { data ->
            if (closing || requestEpoch != epoch) return@invoke
            busy = false
            val url = data?.get("video") as? String
            if (url.isNullOrBlank()) { error = "切换失败，请返回重试"; showControls(); return@invoke }
            @Suppress("UNCHECKED_CAST")
            val headers = data["headers"] as? Map<String, String> ?: emptyMap()
            CinemaSession.media = CinemaMedia(url, data["audio"] as? String,
                (data["cid"] as? Number)?.toLong() ?: 0L, headers, position)
            lastPosition = position
            sourceChanges++
            player?.let { loadMedia(it, wasPlaying && resumed && spatialFocused) }
            showControls()
        }
    }

    private fun setCinema(value: Boolean) = setEnvironment(if(value) "dark" else "pure")
    private fun setEnvironment(value: String) {
        environment=value; cinema=value=="dark" || value=="light"
        // Pure picture hides text without discarding the user's danmaku preference.
        danmaku?.visibility = if (value == "pure") View.INVISIBLE else View.VISIBLE
        scene.enablePassthrough(value=="passthrough")
        room.forEach { it.first.setComponent(Visible(cinema)) }
        skyMaterial?.setAlbedoColor(Color.valueOf(if(value=="light") 0xFF555961.toInt() else Color.BLACK))
        showControls()
    }

    private fun recenter() {
        // Read the tracked viewer directly; no avatar entity is required by this app.
        if (closing) return
        val head = try { scene.getViewerPose() } catch (_: Exception) { return }
        // Anchor once to the current gaze, including reclining pitch. It does not follow head motion.
        val tracked = kotlin.math.abs(head.t.x) + kotlin.math.abs(head.t.y) + kotlin.math.abs(head.t.z) +
            kotlin.math.abs(head.q.x) + kotlin.math.abs(head.q.y) + kotlin.math.abs(head.q.z) > .0001f
        if (!tracked) return
        anchor = head.times(Pose(Vector3(0f, -2f, 0f)))
        alignedAfterVideo = true
        applyLayout()
    }

    private fun applyLayout() {
        video?.setComponents(listOf(Transform(anchor.times(Pose(Vector3(0f, 2f, 4.5f)))),
            Scale(Vector3(screenScale * kotlin.math.min(1f, ratio / (16f / 9f)), screenScale * kotlin.math.min(1f, (16f / 9f) / ratio), 1f))))
        overlay?.setComponents(listOf(Transform(anchor.times(Pose(Vector3(0f, 2f, 4.47f)))), Scale(Vector3(screenScale, screenScale, 1f))))
        room.forEach { (entity, pose) -> entity.setComponent(Transform(anchor.times(pose))) }
    }

    /** An unbounded dark space with a continuous feathered floor light texture. */
    private fun createRoom() {
        val bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader=RadialGradient(256f,256f,252f,intArrayOf(0xFF655D56.toInt(),0xFF252326.toInt(),Color.BLACK),floatArrayOf(0f,.38f,1f),Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f,0f,512f,512f,paint)
        val material=SceneMaterial(SceneTexture(bitmap)).apply { setUnlit(true);setSidedness(MaterialSidedness.DOUBLE_SIDED) }
        floorMaterial=material
        val pose=Pose(Vector3(0f,-.15f,3.5f),Quaternion(90f,0f,0f))
        val entity=Entity.create(listOf(Transform(pose),Visible(cinema)))
        val mesh=SceneMesh.quad(Vector3(-7f,-5f,0f),Vector3(7f,5f,0f),material)
        val obj=SceneObject(scene,mesh,"cinema-soft-floor",entity)
        systemManager.findSystem<SceneObjectSystem>().addSceneObject(entity,CompletableFuture.completedFuture(obj))
        room.add(entity to pose)
        val sky=SceneMaterial(SceneTexture(Color.valueOf(Color.WHITE))).apply {
            setUnlit(true);setSidedness(MaterialSidedness.DOUBLE_SIDED);setAlbedoColor(Color.valueOf(Color.BLACK))
        }
        skyMaterial=sky
        val skyEntity=Entity.create(listOf(Transform(),Visible(cinema)))
        val skyObject=SceneObject(scene,SceneMesh.skybox(60f,sky),"cinema-environment",skyEntity)
        systemManager.findSystem<SceneObjectSystem>().addSceneObject(skyEntity,CompletableFuture.completedFuture(skyObject))
        room.add(skyEntity to Pose())
    }

    private fun clock(ms: Long) = "${ms.coerceAtLeast(0) / 60000}:${(ms.coerceAtLeast(0) / 1000 % 60).toString().padStart(2, '0')}"
    private val tick = object : Runnable {
        override fun run() {
            if (closing) return
            val p = player
            if (firstFrame && !alignedAfterVideo) recenter()
            if (p != null) {
                if (kotlin.math.abs(p.currentPosition - lastPosition) > 2000) danmaku?.comments?.clear()
                lastPosition = p.currentPosition
                status?.text = error ?: if (busy) "切换中…" else "${clock(lastPosition)} / ${clock(p.duration)}"
                playingButton?.update(if(p.isPlaying) "pause" else "play", if(p.isPlaying) "暂停" else "播放")
                if (!dragging && p.duration > 0) progress?.progress = (lastPosition * 1000 / p.duration).toInt()
                if (p.isPlaying && !dragging && !busy && qualityPanel == null && error == null &&
                    SystemClock.uptimeMillis() - lastTouch > 3500) hideControls()
                if (environment != "pure" && danmaku?.danmakuEnabled == true && p.isPlaying && !fetchingDanmaku && ticks % 2 == 0) {
                    fetchingDanmaku = true
                    CinemaSession.request?.invoke("danmaku", mapOf("positionMs" to lastPosition)) { response ->
                        fetchingDanmaku = false
                        if (closing) return@invoke
                        @Suppress("UNCHECKED_CAST")
                        val items = response?.get("items") as? List<Map<String, Any>> ?: emptyList()
                        items.take(3).forEach { item ->
                            val c = CinemaOverlayView.Comment((item["time"] as? Number)?.toLong() ?: 0,
                                item["text"].toString(), (item["color"] as? Number)?.toInt() ?: Color.WHITE)
                            danmaku?.let { if (c !in it.comments && it.comments.size < 8) it.comments.add(c) }
                        }
                    }
                }
                if (++ticks % 10 == 0 && !busy) CinemaSession.request?.invoke("position",
                    mapOf("positionMs" to lastPosition, "cid" to (media?.cid ?: 0L))) { }
            }
            writeStatus()
            if (!closing) handler.postDelayed(this, 500)
        }
    }

    private fun writeStatus() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        filesDir.resolve("cinema-status.json").writeText(JSONObject(mapOf(
            "positionMs" to lastPosition, "playing" to (player?.isPlaying == true),
            "videoFrames" to (player?.videoDecoderCounters?.renderedOutputBufferCount ?: 0),
            "audioBuffers" to (player?.audioDecoderCounters?.renderedOutputBufferCount ?: 0),
            "sceneReady" to (video != null), "firstFrame" to firstFrame,
            "sourceChanges" to sourceChanges, "busy" to busy,
            "trackingAligned" to alignedAfterVideo,
            "danmakuEnabled" to (danmaku?.danmakuEnabled == true),
            "danmakuVisible" to (danmaku?.danmakuEnabled == true && danmaku?.visibility == View.VISIBLE),
            "environment" to environment, "screenScale" to screenScale, "passthrough" to (environment=="passthrough"), "cinema" to cinema, "roomMeshes" to room.size,
            "controlsVisible" to (hud?.visibility == View.VISIBLE), "error" to error
        )).toString())
    }

    override fun onSessionStateChanged(state: SessionState) {
        super.onSessionStateChanged(state)
        spatialFocused = state == SessionState.FOCUSED
        if (!spatialFocused) {
            resumeAfterFocus = resumeAfterFocus || player?.playWhenReady == true; player?.pause()
        } else if (resumed && (resumeAfterFocus || resumePlayback) && !closing) {
            resumeAfterFocus = false; resumePlayback = false; player?.play()
        }
    }
    override fun onPause() { resumed = false; resumePlayback = resumePlayback || player?.playWhenReady == true; player?.pause(); super.onPause() }
    override fun onResume() {
        super.onResume(); resumed = true
        if (spatialFocused && (resumePlayback || resumeAfterFocus) && !closing) {
            resumePlayback = false; resumeAfterFocus = false; player?.play()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("position", player?.currentPosition ?: lastPosition); super.onSaveInstanceState(outState)
    }
    override fun finish() {
        if (!closing) {
            closing = true; lastPosition = player?.currentPosition ?: lastPosition
            player?.pause()
        }
        super.finish()
    }
    override fun onSpatialShutdown() {
        closing = true
        handler.removeCallbacksAndMessages(null)
        lastPosition = player?.currentPosition ?: lastPosition
        player?.release(); player = null
        super.onSpatialShutdown()
    }
    override fun onDestroy() {
        closing = true
        handler.removeCallbacksAndMessages(null)
        lastPosition = player?.currentPosition ?: lastPosition
        player?.release(); player = null
        super.onDestroy()
        if (isFinishing) {
            CinemaSession.complete(lastPosition)
            // Wait for Horizon to tear down the immersive task before restoring the panel.
            val context = applicationContext
            Handler(Looper.getMainLooper()).postDelayed({
                context.startActivity(android.content.Intent(context, MainActivity::class.java).apply {
                    action = android.content.Intent.ACTION_MAIN
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                })
            }, 1200)
        }
    }
}
