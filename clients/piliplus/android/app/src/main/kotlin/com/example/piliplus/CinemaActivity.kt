package com.example.piliplus

import android.content.pm.ApplicationInfo
import android.graphics.Color
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
    private var progress: SeekBar? = null
    private var playingButton: Button? = null
    private var roomButton: Button? = null
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
    private var lastTouch = SystemClock.uptimeMillis()
    private var ticks = 0
    private var fetchingDanmaku = false
    private var cinema = true
    private var error: String? = null
    private val media get() = CinemaSession.media
    private val accent = Color.rgb(251, 114, 153)

    override fun registerFeatures(): List<SpatialFeature> = listOf(VRFeature(this))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (media == null) { finish(); return }
        lastPosition = savedInstanceState?.getLong("position") ?: media!!.positionMs
        cinema = getSharedPreferences("cinema", MODE_PRIVATE).getBoolean("room", true)
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            CinemaSession.testCommand = { command -> runOnUiThread {
                when (command) {
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

    private fun loadMedia(p: ExoPlayer) {
        val m = media ?: return
        val http = DefaultHttpDataSource.Factory().setDefaultRequestProperties(m.headers)
            .setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
        val factory = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this, http))
        val v = factory.createMediaSource(MediaItem.fromUri(m.video))
        val merged = if (m.audio.isNullOrBlank()) v else MergingMediaSource(v,
            factory.createMediaSource(MediaItem.fromUri(m.audio!!)))
        p.setMediaSource(merged); p.seekTo(lastPosition); p.prepare(); p.playWhenReady = true
    }

    private fun dp(value: Int): Int = (value * (root?.resources?.displayMetrics?.density ?: resources.displayMetrics.density) + .5f).toInt()

    private fun buildOverlay(frame: FrameLayout) {
        root = frame
        frame.setOnClickListener {
            if (hud?.visibility == View.VISIBLE && player?.isPlaying == true) hideControls()
            else showControls()
        }
        danmaku = CinemaOverlayView(frame.context) { player?.currentPosition ?: lastPosition }
        frame.addView(danmaku, FrameLayout.LayoutParams(-1, -1))
        hud = LinearLayout(frame.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xDC17171B.toInt(), 0xF5202024.toInt())).apply { cornerRadius = 18f }
        }
        frame.addView(hud, FrameLayout.LayoutParams(-1, dp(142), Gravity.BOTTOM))
        progress = SeekBar(frame.context).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar) { dragging = true; showControls() }
                override fun onStopTrackingTouch(bar: SeekBar) {
                    player?.let { if (it.duration > 0) it.seekTo(it.duration * bar.progress / 1000) }
                    dragging = false; lastTouch = SystemClock.uptimeMillis()
                }
                override fun onProgressChanged(bar: SeekBar, value: Int, user: Boolean) {}
            })
        }
        hud!!.addView(progress, LinearLayout.LayoutParams(-1, dp(40)))
        val row = LinearLayout(frame.context).apply { gravity = Gravity.CENTER_VERTICAL }
        hud!!.addView(row, LinearLayout.LayoutParams(-1, dp(76)))
        fun button(label: String, width: Int = 104, action: () -> Unit): Button {
            return Button(frame.context).apply {
                text = label; textSize = 20f; isAllCaps = false; isSingleLine = true
                setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT)
                setPadding(dp(4), 0, dp(4), 0); contentDescription = label
                setOnClickListener { lastTouch = SystemClock.uptimeMillis(); action() }
                row.addView(this, LinearLayout.LayoutParams(dp(width), dp(68)))
            }
        }
        playingButton = button("暂停", 78) { player?.let {
            if (it.isPlaying) it.pause() else {
                if (it.playbackState == Player.STATE_ENDED) it.seekTo(0)
                it.play()
            }
        } }.apply { setTextColor(accent) }
        button("↶ 10", 78) { player?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) } }
        button("10 ↷", 78) { player?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration.coerceAtLeast(0))) } }
        status = TextView(frame.context).apply { textSize = 16f; setTextColor(Color.LTGRAY) }
        row.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        button("弹幕 关") {
            danmaku?.let { it.danmakuEnabled = !it.danmakuEnabled
                if (!it.danmakuEnabled) it.comments.clear() }
            (row.getChildAt(4) as Button).text = if (danmaku?.danmakuEnabled == true) "弹幕 开" else "弹幕 关"
        }
        button("画质") { showQuality() }
        roomButton = button(if (cinema) "纯画面" else "影院") { setCinema(!cinema) }
        button("居中", 80) { recenter() }
        button("退出全屏", 116) { finish() }
        showControls()
    }

    private fun showControls() { hud?.visibility = View.VISIBLE; lastTouch = SystemClock.uptimeMillis() }
    private fun hideControls() {
        hud?.visibility = View.GONE
        root?.findViewWithTag<View>("quality-menu")?.let { root?.removeView(it) }
        qualityPanel = null
    }
    private fun showQuality() {
        showControls()
        qualityPanel?.let { root?.removeView(it) }
        val panel = LinearLayout(root!!.context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xF5202024.toInt())
        }
        qualityPanel = panel
        val scroll = ScrollView(root!!.context).apply { addView(panel) }
        // Remove a previous menu wrapper before adding another one.
        root!!.findViewWithTag<View>("quality-menu")?.let { root!!.removeView(it) }
        scroll.tag = "quality-menu"
        root!!.addView(scroll, FrameLayout.LayoutParams(dp(350), dp(400), Gravity.RIGHT or Gravity.TOP))
        CinemaSession.request?.invoke("catalog", emptyMap()) { response ->
            if (closing) return@invoke
            @Suppress("UNCHECKED_CAST")
            val qualities = response?.get("qualities") as? List<Map<String, Any>> ?: emptyList()
            for (q in qualities) panel.addView(Button(panel.context).apply {
                text = (if (q["selected"] == true) "✓ " else "") + q["title"]
                textSize = 20f; isAllCaps = false; minHeight = dp(66)
                setOnClickListener { root?.removeView(scroll); qualityPanel = null
                    switchQuality(q["id"] as? Number ?: return@setOnClickListener) }
            })
            panel.addView(Button(panel.context).apply { text = "关闭"; minHeight = dp(60)
                setOnClickListener { root?.removeView(scroll); qualityPanel = null; showControls() } })
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
            player?.let { loadMedia(it); it.playWhenReady = wasPlaying }
            showControls()
        }
    }

    private fun setCinema(value: Boolean) {
        cinema = value
        room.forEach { it.first.setComponent(Visible(value)) }
        roomButton?.text = if (value) "纯画面" else "影院"
        getSharedPreferences("cinema", MODE_PRIVATE).edit().putBoolean("room", value).apply()
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
            Scale(Vector3(kotlin.math.min(1f, ratio / (16f / 9f)), kotlin.math.min(1f, (16f / 9f) / ratio), 1f))))
        overlay?.setComponent(Transform(anchor.times(Pose(Vector3(0f, 2f, 4.47f)))))
        room.forEach { (entity, pose) -> entity.setComponent(Transform(anchor.times(pose))) }
    }

    /** Procedural geometry: real binocular depth, with restrained static spill-light accents. */
    private fun createRoom() {
        val systems = systemManager.findSystem<SceneObjectSystem>()
        fun box(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, color: Int) {
            val pose = Pose(Vector3(x, y, z))
            val entity = Entity.create(listOf(Transform(pose), Visible(cinema)))
            val material = SceneMaterial(SceneTexture(Color.valueOf(color))).apply { setUnlit(true) }
            val mesh = SceneMesh.box(Vector3(-w / 2, -h / 2, -d / 2), Vector3(w / 2, h / 2, d / 2), material)
            val obj = SceneObject(scene, mesh, "cinema-room-${room.size}", entity)
            systems.addSceneObject(entity, CompletableFuture.completedFuture(obj))
            room.add(entity to pose)
        }
        box(0f, -.08f, 2f, 12f, .15f, 16f, 0xFF12131A.toInt())
        box(0f, 2.5f, 5.3f, 12f, 5f, .2f, 0xFF101116.toInt())
        for (side in listOf(-1f, 1f)) {
            box(side * 5f, 2.5f, 0f, .15f, 5f, 11f, 0xFF14151D.toInt())
            for (z in -3..4) {
                box(side * 4.85f, 2.4f, z.toFloat(), .16f, 4.8f, .12f, 0xFF22232D.toInt())
                box(side * 4.73f, .45f, z.toFloat(), .025f, .025f, .55f, 0xFF725462.toInt())
            }
        }
        // Dark screen surround and a shallow stage establish depth without extra UI.
        box(0f, 2f, 4.65f, 5.1f, 2.98f, .14f, 0xFF050507.toInt())
        box(0f, .16f, 4.3f, 6.8f, .3f, 1.6f, 0xFF20212A.toInt())
        box(0f, .32f, 3.53f, 6.7f, .015f, .04f, 0xFF76596A.toInt())
        // Wide, dim bands approximate soft floor glow, not video reflections.
        for (i in 0..11) {
            val t = i / 11f
            val c = (26 + 15 * t).toInt()
            box(0f, .008f + i * .0003f, 2.9f + i * .075f,
                6.5f - i * .14f, .004f, .12f, Color.rgb(c, c - 2, c + 5))
        }
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
                playingButton?.text = if (p.isPlaying) "暂停" else "播放"
                playingButton?.contentDescription = playingButton?.text
                if (!dragging && p.duration > 0) progress?.progress = (lastPosition * 1000 / p.duration).toInt()
                if (p.isPlaying && !dragging && !busy && qualityPanel == null && error == null &&
                    SystemClock.uptimeMillis() - lastTouch > 3500) hideControls()
                if (danmaku?.danmakuEnabled == true && p.isPlaying && !fetchingDanmaku && ticks % 2 == 0) {
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
            "passthrough" to false, "cinema" to cinema, "roomMeshes" to room.size,
            "controlsVisible" to (hud?.visibility == View.VISIBLE), "error" to error
        )).toString())
    }

    override fun onSessionStateChanged(state: SessionState) {
        super.onSessionStateChanged(state)
        if (state == SessionState.VISIBLE) {
            resumeAfterFocus = player?.playWhenReady == true; player?.pause()
        } else if (state == SessionState.FOCUSED && resumeAfterFocus && !closing) {
            resumeAfterFocus = false; player?.play()
        }
    }
    override fun onPause() { resumePlayback = player?.playWhenReady == true; player?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); if (resumePlayback && !closing) player?.play() }
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
        if (isFinishing) CinemaSession.complete(lastPosition)
    }
}
