package com.example.piliplus

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import android.content.pm.ApplicationInfo
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Lut
import com.meta.spatial.core.Query
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Pose
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.toolkit.*
import com.meta.spatial.vr.VRFeature

/** Real native video Surface in OpenXR. All panels are runtime user layout data. */
class SpatialPlayerActivity : AppSystemActivity() {
    private var player: ExoPlayer? = null
    private var video: Entity? = null
    private var controls: Entity? = null
    private var episodes: Entity? = null
    private var comments: Entity? = null
    private var overlay: Entity? = null
    private val handles = arrayOfNulls<Entity>(3)
    private var overlayView: SpatialOverlayView? = null
    private var fetchingDanmaku = false
    private var previousPosition = 0L
    private var episodeRoot: LinearLayout? = null
    private var commentRoot: LinearLayout? = null
    private var showEpisodes = false
    private var showComments = false
    private var viewingSettings = false
    private var environmentBrightness = 1f
    private var savedVolume = 1f
    private var danmakuOn = false
    private var danmakuDensity = 3
    private var danmakuFontScale = 1f
    private var episodeList: List<Map<String, Any>> = emptyList()
    private var catalogCount = 0
    private var qualityCount = 0
    private var commentCharacters = 0
    private var sourceChanges = 0
    private var firstQuality: Any? = null
    private var busy = false
    private var requestEpoch = 0
    private var auxiliaryOpacity = .88f
    private var layoutPreset = "focus"
    private var selectedPanel = 0
    private data class Placement(var x: Float, var y: Float = 1.6f, var z: Float = 2.6f,
        var scale: Float = 1f, var locked: Boolean = false, var follow: Boolean = true)
    private val auxiliary = arrayOf(Placement(-1.85f), Placement(1.85f))
    private var syncTicks = 0
    private val handler = Handler(Looper.getMainLooper())
    private var status: TextView? = null
    private var progress: SeekBar? = null
    private var dragging = false
    private var error: String? = null
    private var distance = 2.5f
    private var scale = 1f
    private var height = 1.6f
    private var horizontal = 0f
    private var locked = false
    private var editing = false
    private var passthrough = true
    private var lastPosition = 0L
    private var closing = false
    private var resumePlayback = false
    private var completed = false
    private var layoutAnchor = Pose()
    private var videoRatio = 16f / 9f
    private val prefs by lazy { getSharedPreferences("spatial_layout", MODE_PRIVATE) }
    private val media get() = SpatialSession.media

    override fun registerFeatures(): List<SpatialFeature> = listOf(VRFeature(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (media == null) { finish(); return }
        distance = prefs.getFloat("distance", 2.5f).coerceIn(1.5f, 3.5f)
        scale = prefs.getFloat("scale", 1f).coerceIn(.5f, 1.5f)
        height = prefs.getFloat("height", 1.6f).coerceIn(.7f, 2.5f)
        horizontal = prefs.getFloat("horizontal", 0f).coerceIn(-2f, 2f)
        locked = prefs.getBoolean("locked", false)
        passthrough = prefs.getBoolean("passthrough", true)
        auxiliaryOpacity = prefs.getFloat("opacity", .88f).coerceIn(.35f, 1f)
        layoutPreset = prefs.getString("preset", "focus") ?: "focus"
        savedVolume = prefs.getFloat("volume", 1f).coerceIn(0f, 1f)
        environmentBrightness = prefs.getFloat("brightness", 1f).coerceIn(.1f, 1f)
        danmakuOn = prefs.getBoolean("danmaku", false)
        danmakuDensity = prefs.getInt("density", 3).coerceIn(1, 5)
        danmakuFontScale = prefs.getFloat("fontScale", 1f).coerceIn(.8f, 1.5f)
        loadPreset()
        showEpisodes = prefs.getBoolean("$layoutPreset.episodes", layoutPreset == "desk")
        showComments = prefs.getBoolean("$layoutPreset.comments", layoutPreset != "focus")
        lastPosition = savedInstanceState?.getLong("position") ?: media!!.positionMs
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            SpatialSession.testCommand = { command -> runOnUiThread {
                when (command) {
                    "pause" -> player?.pause()
                    "play" -> player?.play()
                    "seek" -> player?.seekTo(3000)
                    "mute" -> player?.volume = 0f
                    "finish" -> finish()
                    "panels" -> {
                        showEpisodes = true; showComments = true
                        episodes?.setComponent(Visible(true)); comments?.setComponent(Visible(true))
                        refreshCatalog(); refreshComments()
                    }
                    "quality" -> firstQuality?.let { switchMedia("quality", mapOf("id" to it)) }
                    "smaller" -> { scale = .7f; applyLayout(); saveLayout() }
                }
                writeTestStatus()
            } }
        }
    }

    override fun onSceneReady() {
        super.onSceneReady()
        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        scene.enablePassthrough(passthrough)
        applyEnvironmentBrightness()
        // Position and scale are loaded from the user's saved runtime layout.
        video = Entity.create(listOf(Panel(R.id.spatial_video), Transform(), Scale(Vector3(scale))))
        overlay = Entity.create(listOf(Panel(R.id.spatial_overlay), Transform(), Scale(Vector3(scale))))
        controls = Entity.create(listOf(Panel(R.id.spatial_controls), Transform()))
        episodes = Entity.create(listOf(Panel(R.id.spatial_episodes), Transform(), Visible(showEpisodes)))
        comments = Entity.create(listOf(Panel(R.id.spatial_comments), Transform(), Visible(showComments)))
        for ((i, id) in intArrayOf(R.id.spatial_handle_main, R.id.spatial_handle_episodes, R.id.spatial_handle_comments).withIndex()) {
            handles[i] = Entity.create(listOf(Panel(id), Transform(), Visible(false), Grabbable(false, GrabbableType.PIVOT_Y)))
        }
        recallLayout()
        applyLayout()
        handler.post(dragTick)
        handler.post(tick)
        Log.i(TAG, "scene_ready")
    }

    override fun registerPanels(): List<PanelRegistration> = listOf(
        VideoSurfacePanelRegistration(
            R.id.spatial_video,
            surfaceConsumer = { _, surface ->
                runOnUiThread {
                    if (!closing && media != null) {
                        if (player == null) createPlayer()
                        player?.setVideoSurface(surface)
                    }
                }
            },
            settingsCreator = {
                MediaPanelSettings(
                    shape = QuadShapeOptions(width = 2.4f, height = 1.35f),
                    display = PixelDisplayOptions(width = 1920, height = 1080),
                )
            },
        ),
        LayoutXMLPanelRegistration(
            R.id.spatial_controls,
            layoutIdCreator = { R.layout.spatial_controls },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 2.3f, height = .85f),
                    display = DpDisplayOptions(width = 1000f, height = 370f),
                    style = PanelStyleOptions(themeResourceId = R.style.SpatialPanelTheme),
                )
            },
            panelSetupWithRootView = { view, _, _ ->
                runOnUiThread { buildControls(view.findViewById(R.id.spatial_controls_root)) }
            },
        ),
        handlePanel(R.id.spatial_handle_main, 2.4f),
        handlePanel(R.id.spatial_handle_episodes, .95f),
        handlePanel(R.id.spatial_handle_comments, .95f),
        auxiliaryPanel(R.id.spatial_episodes, true),
        auxiliaryPanel(R.id.spatial_comments, false),
        LayoutXMLPanelRegistration(R.id.spatial_overlay,
            layoutIdCreator = { R.layout.spatial_overlay },
            settingsCreator = { UIPanelSettings(
                shape = QuadShapeOptions(width = 2.4f, height = 1.35f),
                display = DpDisplayOptions(width = 1000f, height = 562.5f),
                style = PanelStyleOptions(themeResourceId = R.style.SpatialOverlayTheme),
            ) },
            panelSetupWithRootView = { root, _, _ -> runOnUiThread {
                val frame = root.findViewById<FrameLayout>(R.id.spatial_overlay_root)
                overlayView = SpatialOverlayView(frame.context) { player?.currentPosition ?: 0L }
                overlayView?.let { it.danmakuEnabled = danmakuOn; it.density = danmakuDensity; it.fontScale = danmakuFontScale }
                frame.addView(overlayView, FrameLayout.LayoutParams(-1, -1))
            } },
        ),
    )

    private fun handlePanel(id: Int, width: Float): PanelRegistration = LayoutXMLPanelRegistration(id,
        layoutIdCreator = { R.layout.spatial_handle },
        settingsCreator = { UIPanelSettings(
            shape = QuadShapeOptions(width = width, height = .12f),
            display = DpDisplayOptions(width = width * 430, height = 52f),
            style = PanelStyleOptions(themeResourceId = R.style.SpatialOverlayTheme),
        ) },
    )

    private fun auxiliaryPanel(id: Int, isEpisodes: Boolean): PanelRegistration =
        LayoutXMLPanelRegistration(id,
            layoutIdCreator = { R.layout.spatial_controls },
            settingsCreator = { UIPanelSettings(
                shape = QuadShapeOptions(width = .95f, height = 1.4f),
                display = DpDisplayOptions(width = 430f, height = 640f),
                style = PanelStyleOptions(themeResourceId = R.style.SpatialOverlayTheme),
            ) },
            panelSetupWithRootView = { view, _, _ -> runOnUiThread {
                view.setBackgroundColor(Color.TRANSPARENT)
                val root = view.findViewById<LinearLayout>(R.id.spatial_controls_root)
                if (isEpisodes) { episodeRoot = root; refreshCatalog() }
                else { commentRoot = root; refreshComments() }
            } },
        )

    private fun auxiliaryText(root: LinearLayout, message: String) {
        root.removeAllViews()
        root.setBackgroundColor(Color.argb((auxiliaryOpacity * 255).toInt(), 21, 26, 40))
        root.addView(TextView(root.context).apply {
            text = message; textSize = 21f; setTextColor(Color.WHITE); setPadding(6, 8, 6, 8)
        })
    }
    private fun refreshCatalog() {
        val root = episodeRoot ?: return
        auxiliaryText(root, "选集与画质\n正在加载…")
        SpatialSession.request?.invoke("catalog", emptyMap()) { data ->
            if (closing) return@invoke
            auxiliaryText(root, "选集与画质")
            if (data == null) { auxiliaryText(root, "暂无法加载，返回内容页重试"); return@invoke }
            @Suppress("UNCHECKED_CAST")
            val items = data["episodes"] as? List<Map<String, Any>> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val qualities = data["qualities"] as? List<Map<String, Any>> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val subtitles = data["subtitles"] as? List<Map<String, Any>> ?: emptyList()
            episodeList = items
            catalogCount = items.size; qualityCount = qualities.size
            firstQuality = qualities.lastOrNull()?.get("id")
            fun entry(label: String, method: String, args: Map<String, Any>) {
                root.addView(Button(root.context).apply {
                    text = label; textSize = 19f; isAllCaps = false; minHeight = 60
                    setOnClickListener { switchMedia(method, args) }
                })
            }
            for (item in items) {
                val cid = item["cid"] ?: continue
                entry((if (item["selected"] == true) "▶ " else "") + item["title"], "select", mapOf("cid" to cid))
            }
            for (item in qualities) {
                val id = item["id"] ?: continue
                entry(item["title"].toString(), "quality", mapOf("id" to id))
            }
            for (item in subtitles) {
                root.addView(Button(root.context).apply {
                    text = "字幕：${item["title"]}"; textSize = 19f
                    setOnClickListener {
                        SpatialSession.request?.invoke("subtitle", mapOf("id" to (item["id"] ?: 0))) { response ->
                            @Suppress("UNCHECKED_CAST")
                            val cues = response?.get("items") as? List<Map<String, Any>> ?: emptyList()
                            overlayView?.captions = cues.mapNotNull { cue ->
                                val from = (cue["from"] as? Number)?.toDouble() ?: return@mapNotNull null
                                val to = (cue["to"] as? Number)?.toDouble() ?: return@mapNotNull null
                                SpatialOverlayView.Caption((from * 1000).toLong(), (to * 1000).toLong(), cue["text"].toString())
                            }
                        }
                    }
                })
            }
        }
    }
    private fun refreshComments() {
        val root = commentRoot ?: return
        auxiliaryText(root, "评论\n正在加载…")
        SpatialSession.request?.invoke("comments", emptyMap()) { data ->
            if (!closing) {
                commentCharacters = (data?.get("text") as? String)?.length ?: 0
                auxiliaryText(root, "评论\n\n" + (data?.get("text") ?: "暂无法加载，可返回内容页查看"))
            }
        }
    }
    private fun switchMedia(method: String, args: Map<String, Any>) {
        if (busy || closing) return
        busy = true
        val epoch = ++requestEpoch
        handler.postDelayed({
            if (requestEpoch == epoch && busy && !closing) {
                requestEpoch++; busy = false; error = "请求超时，可重试或返回内容页"
            }
        }, 30000)
        val oldPosition = player?.currentPosition ?: 0L
        player?.pause()
        status?.text = "正在切换…"
        SpatialSession.request?.invoke(method, args) { data ->
            if (requestEpoch != epoch) return@invoke
            busy = false
            if (closing) return@invoke
            val url = data?.get("video") as? String
            if (url.isNullOrBlank()) { error = "切换失败，可重试或返回内容页"; return@invoke }
            @Suppress("UNCHECKED_CAST")
            val headers = data["headers"] as? Map<String, String> ?: emptyMap()
            SpatialSession.media = SpatialMedia(url, data["audio"] as? String,
                data["title"] as? String ?: "VRBiliBili", (data["cid"] as? Number)?.toLong() ?: 0,
                headers, if (method == "select") 0 else oldPosition)
            sourceChanges++
            error = null; completed = false
            overlayView?.comments?.clear(); overlayView?.captions = emptyList()
            lastPosition = media!!.positionMs
            player?.let { loadMedia(it) }
            refreshCatalog(); refreshComments()
        }
    }

    private fun createPlayer() {
        player = ExoPlayer.Builder(this).build().also { p ->
            p.volume = savedVolume
            p.addListener(object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) {
                    error = "播放失败（${e.errorCode}），可重试或返回刷新片源"
                    Log.w(TAG, "player_error code=${e.errorCode}")
                }
                override fun onRenderedFirstFrame() { Log.i(TAG, "first_video_frame") }
                override fun onVideoSizeChanged(size: VideoSize) {
                    if (size.width > 0 && size.height > 0) {
                        videoRatio = size.width * size.pixelWidthHeightRatio / size.height
                        applyLayout()
                    }
                }
                override fun onPlaybackStateChanged(state: Int) {
                    completed = state == Player.STATE_ENDED
                    Log.i(TAG, "playback_state=$state")
                }
            })
            loadMedia(p)
        }
    }
    private fun loadMedia(p: ExoPlayer) {
        val item = media ?: return
        val http = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(item.headers)
            .setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
        val source = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this, http))
        val videoSource = source.createMediaSource(MediaItem.fromUri(item.video))
        val merged = if (!item.audio.isNullOrBlank()) MergingMediaSource(
            videoSource, source.createMediaSource(MediaItem.fromUri(item.audio!!))
        ) else videoSource
            p.setMediaSource(merged)
            p.seekTo(lastPosition.coerceAtLeast(0L))
            p.prepare()
            p.playWhenReady = true
    }

    private fun buildControls(root: LinearLayout) {
        fun text(value: String, size: Float = 20f) = TextView(root.context).apply {
            text = value; textSize = size; setTextColor(Color.WHITE)
            setPadding(8, 4, 8, 4)
        }
        fun row(): LinearLayout = LinearLayout(root.context).also {
            it.orientation = LinearLayout.HORIZONTAL; root.addView(it)
        }
        fun button(row: LinearLayout, label: String, action: () -> Unit) {
            row.addView(Button(root.context).apply {
                text = label; textSize = 17f; isAllCaps = false; minHeight = 58
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, 62, 1f))
        }
        root.removeAllViews()
        root.addView(text(media?.title ?: "VRBiliBili 空间观看", 24f))
        status = text("正在加载视频…").also { root.addView(it) }
        progress = SeekBar(root.context).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar) { dragging = true }
                override fun onStopTrackingTouch(bar: SeekBar) {
                    player?.let { if (it.duration > 0) it.seekTo(it.duration * bar.progress / 1000) }
                    dragging = false
                }
                override fun onProgressChanged(bar: SeekBar, value: Int, user: Boolean) {}
            })
            root.addView(this, LinearLayout.LayoutParams(-1, 40))
        }
        val playback = row()
        button(playback, "播放 / 暂停") { player?.let { if (it.isPlaying) it.pause() else { if (completed) it.seekTo(0); it.play() } } }
        button(playback, "后退 10 秒") { player?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) } }
        button(playback, "前进 10 秒") { player?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration.coerceAtLeast(0))) } }
        button(playback, "倍速") { player?.let { val speeds = listOf(.75f, 1f, 1.25f, 1.5f, 2f); it.setPlaybackSpeed(speeds[(speeds.indexOf(it.playbackParameters.speed) + 1) % speeds.size]) } }
        button(playback, "重试") {
            if (media?.cid == 0L) { error = null; player?.prepare() }
            else switchMedia("refresh", emptyMap())
        }
        button(playback, "返回内容") { finish() }
        val layout = row()
        button(layout, if (editing) "结束摆放" else "摆放") { editing = !editing; applyLayout(); buildControls(root) }
        button(layout, if (locked) "解锁主屏" else "锁定主屏") { locked = !locked; applyLayout(); saveLayout(); buildControls(root) }
        button(layout, "召回窗口") { recallLayout() }
        button(layout, if (passthrough) "影院" else "透视") { passthrough = !passthrough; scene.enablePassthrough(passthrough); saveLayout(); buildControls(root) }
        button(layout, "观看设置") { viewingSettings = !viewingSettings; buildControls(root) }
        val components = row()
        button(components, "选集 / 画质") { showEpisodes = !showEpisodes; episodes?.setComponent(Visible(showEpisodes)); if (showEpisodes) refreshCatalog(); applyLayout(); saveLayout() }
        button(components, "评论") { showComments = !showComments; comments?.setComponent(Visible(showComments)); if (showComments) refreshComments(); applyLayout(); saveLayout() }
        button(components, "专注 / 桌面 / 陪伴") {
            saveLayout()
            layoutPreset = when (layoutPreset) { "focus" -> "desk"; "desk" -> "social"; else -> "focus" }
            loadPreset(); applyLayout()
            showEpisodes = prefs.getBoolean("$layoutPreset.episodes", layoutPreset == "desk"); showComments = prefs.getBoolean("$layoutPreset.comments", layoutPreset != "focus")
            episodes?.setComponent(Visible(showEpisodes)); comments?.setComponent(Visible(showComments)); saveLayout()
        }
        button(components, "面板背景") {
            auxiliaryOpacity = if (auxiliaryOpacity > .9f) .35f else if (auxiliaryOpacity < .6f) .88f else 1f
            for (panel in listOfNotNull(episodeRoot, commentRoot)) panel.setBackgroundColor(Color.argb((auxiliaryOpacity * 255).toInt(), 21, 26, 40))
            saveLayout()
        }
        button(components, if (danmakuOn) "关闭弹幕" else "开启弹幕") { danmakuOn = !danmakuOn; overlayView?.let { it.danmakuEnabled = danmakuOn; if (!danmakuOn) it.comments.clear() }; saveLayout(); buildControls(root) }
        button(components, "关闭字幕") { overlayView?.captions = emptyList() }
        if (viewingSettings) {
            fun slider(label: String, value: Float, changed: (Float) -> Unit) {
                root.addView(text(label, 18f))
                root.addView(SeekBar(root.context).apply {
                    max = 100; progress = (value * 100).toInt()
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onStartTrackingTouch(bar: SeekBar) { }
                        override fun onStopTrackingTouch(bar: SeekBar) { saveLayout() }
                        override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                            if (fromUser) changed(progress / 100f)
                        }
                    })
                }, LinearLayout.LayoutParams(-1, 48))
            }
            slider("音量", savedVolume) { savedVolume = it; player?.volume = it }
            slider("环境明暗（仅透视）", environmentBrightness) { environmentBrightness = it.coerceAtLeast(.1f); applyEnvironmentBrightness() }
            val extra = row()
            button(extra, "上一段") { adjacentEpisode(-1) }
            button(extra, "下一段") { adjacentEpisode(1) }
            button(extra, "弹幕密度：$danmakuDensity") { danmakuDensity = if (danmakuDensity >= 5) 1 else danmakuDensity + 2; overlayView?.density = danmakuDensity; saveLayout(); buildControls(root) }
            button(extra, "弹幕字号") { danmakuFontScale = if (danmakuFontScale >= 1.5f) .8f else danmakuFontScale + .2f; overlayView?.fontScale = danmakuFontScale; saveLayout() }
        }
        if (editing) {
            val target = row()
            button(target, "主屏") { selectedPanel = 0 }
            button(target, "选集面板") { selectedPanel = 1; showEpisodes = true; episodes?.setComponent(Visible(true)) }
            button(target, "评论面板") { selectedPanel = 2; showComments = true; comments?.setComponent(Visible(true)) }
            button(target, "锁定 / 解锁当前") { if (selectedPanel == 0) locked = !locked else auxiliary[selectedPanel - 1].let { it.locked = !it.locked }; applyLayout(); saveLayout() }
            button(target, "跟随 / 留在空间") { if (selectedPanel > 0) auxiliary[selectedPanel - 1].let { it.follow = !it.follow }; saveLayout() }
            button(target, "恢复此布局") { resetPreset(); applyLayout(); saveLayout() }
            val adjust = row()
            button(adjust, "左") { adjustSelected(dx = -.15f) }
            button(adjust, "右") { adjustSelected(dx = .15f) }
            button(adjust, "上") { adjustSelected(dy = .1f) }
            button(adjust, "下") { adjustSelected(dy = -.1f) }
            button(adjust, "近") { adjustSelected(dz = -.2f) }
            button(adjust, "远") { adjustSelected(dz = .2f) }
            button(adjust, "缩小") { adjustSelected(ds = -.1f) }
            button(adjust, "放大") { adjustSelected(ds = .1f) }
        }
    }

    private fun adjacentEpisode(offset: Int) {
        val index = episodeList.indexOfFirst { (it["cid"] as? Number)?.toLong() == media?.cid }
        val next = episodeList.getOrNull(index + offset)
        if (index < 0 || next == null) { status?.text = "没有更多分集"; return }
        switchMedia("select", mapOf("cid" to (next["cid"] ?: return)))
    }

    private fun applyEnvironmentBrightness() {
        val lut = Lut()
        for (r in 0..15) for (g in 0..15) for (b in 0..15) {
            lut.setMapping(r, g, b, (r * 17 * environmentBrightness).toInt(),
                (g * 17 * environmentBrightness).toInt(), (b * 17 * environmentBrightness).toInt())
        }
        try { scene.setPassthroughLUT(lut) }
        catch (_: Exception) { status?.text = "当前系统无法调整透视亮度" }
    }

    private fun adjustSelected(dx: Float = 0f, dy: Float = 0f, dz: Float = 0f, ds: Float = 0f) {
        if (selectedPanel == 0) {
            if (locked) return
            val x = (horizontal + dx).coerceIn(-2f, 2f)
            val y = (height + dy).coerceIn(.7f, 2.5f)
            val z = (distance + dz).coerceIn(1.5f, 3.5f)
            for (p in auxiliary) if (p.follow && !p.locked) {
                p.x = (p.x + x - horizontal).coerceIn(-3f, 3f)
                p.y = (p.y + y - height).coerceIn(.6f, 2.7f)
                p.z = (p.z + z - distance).coerceIn(1.5f, 3.5f)
            }
            horizontal = x; height = y; distance = z
            scale = (scale + ds).coerceIn(.5f, 1.5f)
        } else {
            val p = auxiliary[selectedPanel - 1]
            if (p.locked) return
            p.x = (p.x + dx).coerceIn(-3f, 3f); p.y = (p.y + dy).coerceIn(.6f, 2.7f)
            p.z = (p.z + dz).coerceIn(1.5f, 3.5f); p.scale = (p.scale + ds).coerceIn(.5f, 1.5f)
        }
        applyLayout(); saveLayout()
    }
    private fun resetPreset() {
        horizontal = 0f; height = 1.6f; distance = 2.5f; scale = 1f; locked = false
        auxiliary[0] = Placement(-1.85f); auxiliary[1] = Placement(1.85f)
    }
    private fun loadPreset() {
        resetPreset()
        fun value(key: String, fallback: Float, min: Float, max: Float): Float {
            val v = prefs.getFloat("$layoutPreset.$key", fallback)
            return if (v.isFinite()) v.coerceIn(min, max) else fallback
        }
        horizontal = value("x", 0f, -2f, 2f); height = value("y", 1.6f, .7f, 2.5f)
        distance = value("z", 2.5f, 1.5f, 3.5f); scale = value("s", 1f, .5f, 1.5f)
        locked = prefs.getBoolean("$layoutPreset.lock", false)
        for ((i, p) in auxiliary.withIndex()) {
            p.x = value("$i.x", p.x, -3f, 3f); p.y = value("$i.y", p.y, .6f, 2.7f)
            p.z = value("$i.z", p.z, 1.5f, 3.5f); p.scale = value("$i.s", 1f, .5f, 1.5f)
            p.locked = prefs.getBoolean("$layoutPreset.$i.lock", false)
            p.follow = prefs.getBoolean("$layoutPreset.$i.follow", true)
        }
    }

    private fun recallLayout() {
        val head = Query.where { has(AvatarAttachment.id) }
            .filter { isLocal() and by(AvatarAttachment.typeData).isEqualTo("head") }
            .eval().firstOrNull()?.getComponent<Transform>()?.transform ?: return
        val forward = head.forward().apply { y = 0f }
        if (forward.x * forward.x + forward.z * forward.z < .0001f) return
        layoutAnchor = Pose(Vector3(head.t.x, head.t.y - 1.6f, head.t.z), Quaternion.lookRotation(forward.normalize()))
        applyLayout()
    }

    private fun placed(x: Float, y: Float, z: Float): Pose = layoutAnchor.times(Pose(Vector3(x, y, z)))

    private fun applyLayout() {
        val fitX = kotlin.math.min(1f, videoRatio / (16f / 9f))
        val fitY = kotlin.math.min(1f, (16f / 9f) / videoRatio)
        video?.setComponents(listOf(Transform(placed(horizontal, height, distance)), Scale(Vector3(scale * fitX, scale * fitY, scale))))
        overlay?.setComponents(listOf(Transform(placed(horizontal, height, distance - .015f)), Scale(Vector3(scale))))
        controls?.setComponent(Transform(placed(0f, .55f, 2.4f)))
        for ((i, entity) in arrayOf(episodes, comments).withIndex()) {
            val p = auxiliary[i]
            entity?.setComponents(listOf(Transform(placed(p.x, p.y, p.z)), Scale(Vector3(p.scale))))
        }
        for ((i, handle) in handles.withIndex()) {
            val p = if (i == 0) Placement(horizontal, height, distance, scale, locked) else auxiliary[i - 1]
            val visible = editing && (i == 0 || if (i == 1) showEpisodes else showComments)
            handle?.setComponents(listOf(Transform(placed(p.x, p.y + (if (i == 0) .735f else .76f) * p.scale, p.z)),
                Scale(Vector3(p.scale)), Visible(visible), Grabbable(visible && !p.locked, GrabbableType.PIVOT_Y)))
        }
    }
    private val dragTick = object : Runnable {
        override fun run() {
            if (editing) for ((i, handle) in handles.withIndex()) {
                val p = if (i == 0) Placement(horizontal, height, distance, scale, locked) else auxiliary[i - 1]
                if (p.locked || handle == null || (i == 1 && !showEpisodes) || (i == 2 && !showComments)) continue
                val actual = layoutAnchor.inverse().times(handle.getComponent<Transform>().transform).t
                val dy = actual.y - (if (i == 0) .735f else .76f) * p.scale - p.y
                val dx = actual.x - p.x; val dz = actual.z - p.z
                if (kotlin.math.abs(dx) + kotlin.math.abs(dy) + kotlin.math.abs(dz) > .005f) {
                    val before = selectedPanel; selectedPanel = i
                    adjustSelected(dx, dy, dz); selectedPanel = before
                }
            }
            if (!closing) handler.postDelayed(this, 33)
        }
    }
    private fun saveLayout() {
        val editor = prefs.edit().putFloat("volume", savedVolume).putFloat("brightness", environmentBrightness)
            .putBoolean("danmaku", danmakuOn).putInt("density", danmakuDensity).putFloat("fontScale", danmakuFontScale)
            .putBoolean("$layoutPreset.episodes", showEpisodes).putBoolean("$layoutPreset.comments", showComments).putFloat("$layoutPreset.x", horizontal).putFloat("$layoutPreset.y", height)
            .putFloat("$layoutPreset.z", distance).putFloat("$layoutPreset.s", scale).putBoolean("$layoutPreset.lock", locked)
        for ((i, p) in auxiliary.withIndex()) {
            editor.putFloat("$layoutPreset.$i.x", p.x).putFloat("$layoutPreset.$i.y", p.y)
                .putFloat("$layoutPreset.$i.z", p.z).putFloat("$layoutPreset.$i.s", p.scale)
                .putBoolean("$layoutPreset.$i.lock", p.locked).putBoolean("$layoutPreset.$i.follow", p.follow)
        }
        editor.putFloat("distance", distance).putFloat("scale", scale)
        .putFloat("height", height).putFloat("horizontal", horizontal).putBoolean("locked", locked)
        .putBoolean("passthrough", passthrough).putFloat("opacity", auxiliaryOpacity)
        .putString("preset", layoutPreset).apply() }
    private fun clock(ms: Long): String = "${ms.coerceAtLeast(0) / 60000}:${(ms.coerceAtLeast(0) / 1000 % 60).toString().padStart(2, '0')}"
    private fun writeTestStatus() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val p = player
        filesDir.resolve("spatial-status.json").writeText(JSONObject(mapOf(
            "cid" to (media?.cid ?: 0L), "catalogCount" to catalogCount,
            "qualityCount" to qualityCount, "commentCharacters" to commentCharacters,
            "sourceChanges" to sourceChanges, "busy" to busy,
            "positionMs" to (p?.currentPosition ?: lastPosition),
            "durationMs" to (p?.duration ?: 0), "playing" to (p?.isPlaying ?: false),
            "state" to (p?.playbackState ?: 0), "error" to error,
            "videoFrames" to (p?.videoDecoderCounters?.renderedOutputBufferCount ?: 0),
            "audioBuffers" to (p?.audioDecoderCounters?.renderedOutputBufferCount ?: 0),
            "videoWidth" to (p?.videoSize?.width ?: 0), "scale" to scale,
            "spatialSceneReady" to (video != null),
        )).toString())
    }
    private val tick = object : Runnable {
        override fun run() {
            player?.let { p ->
                lastPosition = p.currentPosition
                if (kotlin.math.abs(lastPosition - previousPosition) > 2000) overlayView?.comments?.clear()
                previousPosition = lastPosition
                if (overlayView?.danmakuEnabled == true && p.isPlaying && !fetchingDanmaku && syncTicks % 2 == 0) {
                    fetchingDanmaku = true
                    val requestedCid = media?.cid
                    SpatialSession.request?.invoke("danmaku", mapOf("positionMs" to lastPosition)) { response ->
                        fetchingDanmaku = false
                        if (closing || media?.cid != requestedCid) return@invoke
                        @Suppress("UNCHECKED_CAST")
                        val items = response?.get("items") as? List<Map<String, Any>> ?: emptyList()
                        for (item in items.take(3)) {
                            val comment = SpatialOverlayView.Comment((item["time"] as? Number)?.toLong() ?: 0,
                                item["text"].toString(), (item["color"] as? Number)?.toInt() ?: Color.WHITE)
                            overlayView?.let { if (comment !in it.comments && it.comments.size < 8) it.comments.add(comment) }
                        }
                    }
                }
                status?.text = error ?: "${if (completed) "已结束" else if (p.isPlaying) "播放中" else "已暂停 / 缓冲"}  ${clock(p.currentPosition)} / ${clock(p.duration)}  ${p.playbackParameters.speed}×"
                if (!dragging && p.duration > 0) progress?.progress = (p.currentPosition * 1000 / p.duration).toInt()
                if (++syncTicks % 10 == 0 && !busy) {
                    SpatialSession.request?.invoke("position", mapOf("positionMs" to lastPosition, "cid" to (media?.cid ?: 0L))) { }
                    Log.i(TAG, "position_ms=$lastPosition playing=${p.isPlaying}")
                }
            }
            writeTestStatus()
            if (!closing) handler.postDelayed(this, 500)
        }
    }
    override fun onPause() {
        resumePlayback = player?.playWhenReady == true
        player?.pause()
        super.onPause()
    }
    override fun onResume() { super.onResume(); if (resumePlayback && !closing) player?.play() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("position", player?.currentPosition ?: lastPosition)
        super.onSaveInstanceState(outState)
    }
    override fun finish() {
        closing = true
        lastPosition = player?.currentPosition ?: lastPosition
        player?.pause()
        SpatialSession.complete(if (completed) 0 else lastPosition)
        super.finish()
    }
    override fun onDestroy() {
        SpatialSession.testCommand = null
        handler.removeCallbacksAndMessages(null)
        lastPosition = player?.currentPosition ?: lastPosition
        player?.release(); player = null
        if (isFinishing) SpatialSession.complete(lastPosition)
        super.onDestroy()
    }
    companion object { private const val TAG = "VRBiliSpatial" }
}
