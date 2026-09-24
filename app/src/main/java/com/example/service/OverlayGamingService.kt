package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MemeMicApp
import com.example.data.MemeSoundEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayGamingService : Service() {
    private val TAG = "OverlayGamingService"
    private val CHANNEL_ID = "mememic_gaming_service"
    private val NOTIFICATION_ID = 7771

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var overlaySize = "Medium" // Small, Medium, Large
    private var overlayOpacity = 0.90f
    private var snapToEdge = true

    private var quickSounds: List<MemeSoundEntity> = emptyList()

    companion object {
        const val ACTION_START = "com.example.mememic.ACTION_START"
        const val ACTION_STOP = "com.example.mememic.ACTION_STOP"
        const val ACTION_TOGGLE_MUTE = "com.example.mememic.ACTION_TOGGLE_MUTE"

        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? MemeMicApp
        val repo = app?.repository

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MUTE -> {
                val currentMute = app?.liveAudioMixer?.isMicMuted?.value ?: false
                val newMute = !currentMute
                app?.liveAudioMixer?.setMicMuted(newMute)
                serviceScope.launch {
                    repo?.setMicMuted(newMute)
                }
                updateNotification()
                updateOverlayViewContent()
                return START_STICKY
            }
            else -> {
                startAsForeground()
                setupOverlay()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MemeMic Gaming Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running background low-latency meme triggers and floating overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val app = application as? MemeMicApp
        val isMuted = app?.liveAudioMixer?.isMicMuted?.value ?: false

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayGamingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, OverlayGamingService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            2,
            muteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MemeMic Gaming Mode is active")
            .setContentText(if (isMuted) "Microphone is MUTED • Overlay Ready" else "Live Audio & Floating Overlay Ready")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                if (isMuted) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode,
                if (isMuted) "Unmute Mic" else "Mute Mic",
                mutePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Gaming Mode",
                stopPendingIntent
            )
            .build()
    }

    @SuppressLint("RtlHardcoded")
    private fun setupOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot draw overlay: permission not granted")
            return
        }

        val app = application as? MemeMicApp
        serviceScope.launch {
            val settings = app?.repository?.userSettings?.first()
            if (settings != null) {
                overlaySize = settings.overlaySize
                overlayOpacity = settings.overlayOpacity
                snapToEdge = settings.overlaySnapToEdge
            }

            // Collect quick sounds
            app?.repository?.quickAccessSounds?.collect { sounds ->
                quickSounds = if (sounds.isNotEmpty()) sounds else app.repository.favoriteSounds.first().take(6)
                withContext(Dispatchers.Main) {
                    if (overlayView == null) {
                        createOverlayView()
                    } else {
                        updateOverlayViewContent()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        if (overlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 200
        }

        val root = FrameLayout(this)
        overlayView = root

        renderOverlay(root)

        try {
            windowManager?.addView(root, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add window overlay", e)
        }
    }

    private fun updateOverlayViewContent() {
        val root = overlayView as? FrameLayout ?: return
        root.removeAllViews()
        renderOverlay(root)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderOverlay(root: FrameLayout) {
        val scale = resources.displayMetrics.density
        root.alpha = overlayOpacity

        val scaleFactor = when (overlaySize) {
            "Small" -> 0.85f
            "Large" -> 1.15f
            else -> 1.0f
        }

        if (!isExpanded) {
            // Minimized Floating Circle
            val circleSize = (56 * scale * scaleFactor).toInt()
            val circle = FrameLayout(this).apply {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#121927"))
                    setStroke((2.5f * scale).toInt(), Color.parseColor("#00E5FF"))
                }
                background = bg
                elevation = 8f * scale
            }

            val icon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(Color.parseColor("#00E5FF"))
                val iconPadding = (12 * scale * scaleFactor).toInt()
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            }
            circle.addView(icon, FrameLayout.LayoutParams(circleSize, circleSize))

            // Pulse ring / small glow badge
            val app = application as? MemeMicApp
            val isMuted = app?.liveAudioMixer?.isMicMuted?.value ?: false
            if (isMuted) {
                val muteDot = View(this).apply {
                    val dotBg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#FF1744"))
                    }
                    background = dotBg
                }
                val dotSize = (14 * scale).toInt()
                val dotParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = (4 * scale).toInt()
                    rightMargin = (4 * scale).toInt()
                }
                circle.addView(muteDot, dotParams)
            }

            circle.setOnClickListener {
                isExpanded = true
                updateOverlayViewContent()
            }

            attachDragListener(circle)
            root.addView(circle, FrameLayout.LayoutParams(circleSize, circleSize))
        } else {
            // Expanded Gaming Floating Sound Deck
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f * scale
                    setColor(Color.parseColor("#E60F1522"))
                    setStroke((1.5f * scale).toInt(), Color.parseColor("#00E5FF"))
                }
                background = bg
                val pad = (10 * scale * scaleFactor).toInt()
                setPadding(pad, pad, pad, pad)
                elevation = 12f * scale
            }

            // Top Header: Title, Mic Toggle, Collapse, Close
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val title = TextView(this).apply {
                text = "⚡ MemeMic"
                textSize = 13f * scaleFactor
                setTextColor(Color.parseColor("#00E5FF"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f))

            // Mic Mute Button
            val app = application as? MemeMicApp
            val isMuted = app?.liveAudioMixer?.isMicMuted?.value ?: false
            val micBtn = ImageView(this).apply {
                setImageResource(if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_btn_speak_now)
                setColorFilter(if (isMuted) Color.parseColor("#FF1744") else Color.parseColor("#00E676"))
                val p = (6 * scale).toInt()
                setPadding(p, p, p, p)
                setOnClickListener {
                    val toggled = app?.liveAudioMixer?.toggleMicMute() ?: false
                    serviceScope.launch {
                        app?.repository?.setMicMuted(toggled)
                    }
                    updateOverlayViewContent()
                    updateNotification()
                }
            }
            header.addView(micBtn, LinearLayout.LayoutParams((32 * scale).toInt(), (32 * scale).toInt()))

            // Collapse Button
            val collapseBtn = ImageView(this).apply {
                setImageResource(android.R.drawable.arrow_down_float)
                setColorFilter(Color.parseColor("#94A3B8"))
                val p = (6 * scale).toInt()
                setPadding(p, p, p, p)
                setOnClickListener {
                    isExpanded = false
                    updateOverlayViewContent()
                }
            }
            header.addView(collapseBtn, LinearLayout.LayoutParams((32 * scale).toInt(), (32 * scale).toInt()))

            container.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

            // Quick Sound Buttons Grid (2 columns)
            val buttonsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val topMargin = (8 * scale).toInt()
                setPadding(0, topMargin, 0, 0)
            }

            val soundsToDisplay = quickSounds.take(6)
            val rows = soundsToDisplay.chunked(2)

            for (rowSounds in rows) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val rowMargin = (4 * scale).toInt()
                    setPadding(0, rowMargin, 0, rowMargin)
                }

                for (sound in rowSounds) {
                    val btn = createQuickSoundButton(sound, scaleFactor, scale)
                    val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                        val m = (3 * scale).toInt()
                        setMargins(m, 0, m, 0)
                    }
                    rowLayout.addView(btn, params)
                }

                // If odd number, add spacer
                if (rowSounds.size == 1) {
                    val spacer = View(this)
                    rowLayout.addView(spacer, LinearLayout.LayoutParams(0, 0, 1.0f))
                }

                buttonsLayout.addView(rowLayout)
            }

            container.addView(buttonsLayout)

            attachDragListener(header)
            root.addView(container, FrameLayout.LayoutParams(
                (230 * scale * scaleFactor).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun createQuickSoundButton(sound: MemeSoundEntity, scaleFactor: Float, scale: Float): View {
        val app = application as? MemeMicApp
        val btn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * scale
                setColor(Color.parseColor("#1B2438"))
                setStroke((1f * scale).toInt(), Color.parseColor("#283654"))
            }
            background = bg
            val p = (8 * scale * scaleFactor).toInt()
            setPadding(p, p, p, p)
        }

        val dot = View(this).apply {
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E5FF"))
            }
            background = dotBg
        }
        val dotSize = (8 * scale).toInt()
        btn.addView(dot, LinearLayout.LayoutParams(dotSize, dotSize).apply {
            rightMargin = (6 * scale).toInt()
        })

        val text = TextView(this).apply {
            text = sound.name
            textSize = 11f * scaleFactor
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        btn.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f))

        btn.setOnClickListener {
            // Flash color animation
            (btn.background as? GradientDrawable)?.setColor(Color.parseColor("#00E5FF"))
            text.setTextColor(Color.BLACK)

            app?.soundPlayer?.playSound(sound) {
                btn.post {
                    (btn.background as? GradientDrawable)?.setColor(Color.parseColor("#1B2438"))
                    text.setTextColor(Color.WHITE)
                }
            }

            serviceScope.launch {
                app?.repository?.recordSoundPlayed(sound.id)
            }

            btn.postDelayed({
                (btn.background as? GradientDrawable)?.setColor(Color.parseColor("#1B2438"))
                text.setTextColor(Color.WHITE)
            }, 300)
        }

        return btn
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        view.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        hasMoved = true
                    }
                    lp.x = initialX + dx
                    lp.y = initialY + dy
                    windowManager?.updateViewLayout(overlayView, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        view.performClick()
                    } else if (snapToEdge) {
                        snapToNearestEdge()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToNearestEdge() {
        val lp = layoutParams ?: return
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val middle = screenWidth / 2

        lp.x = if (lp.x + 80 < middle) 16 else screenWidth - (overlayView?.width ?: 120) - 16
        lp.x = lp.x.coerceIn(10, screenWidth - 100)
        lp.y = lp.y.coerceIn(50, metrics.heightPixels - 150)

        windowManager?.updateViewLayout(overlayView, lp)

        // Save position to settings
        val app = application as? MemeMicApp
        serviceScope.launch {
            app?.repository?.updateOverlayPosition(lp.x, lp.y)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
            overlayView = null
        }
        serviceScope.cancel()
    }
}
