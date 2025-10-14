package com.example.nuditydetectionapp

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class ScreenGuardService : Service() {

    private lateinit var projection: MediaProjection
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var overlay: OverlayController
    private var classifier: NudityClassifier? = null

    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

  // Processing cadence & anti-flicker
    private var lastProcessMs = 0L
    private val frameIntervalMs = 500L
    private var lastHitMs = 0L
    private var overlayShown = false
  private val holdMs = 600L                   // keep overlay a bit after last hit

    private var screenReceiver: BroadcastReceiver? = null
    private var projectionCallback: MediaProjection.Callback? = null

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayController(this)
        classifier = NudityClassifier(this)
        startForeground(1, buildNotif("Nudity Guard is running"))
        registerScreenOffReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("resultData") ?: return START_NOT_STICKY

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(code, data)

    // Stop service if projection gets revoked by the system
        projectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }.also { projection.registerCallback(it, null) }

        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val d = dm.densityDpi

    // RGBA_8888 is correct (keep this)
        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)

    // IMPORTANT: use AUTO_MIRROR (more reliable for app mirroring than PUBLIC on some builds)
        virtualDisplay = projection.createVirtualDisplay(
            "screen_cap",
            w, h, d,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,   // <— changed from PUBLIC
            imageReader!!.surface, null, null
        )

    // DEBUG: flash the overlay for 1.5s to prove it can be shown
        overlay.show()
        Handler(mainLooper).postDelayed({ overlay.hide() }, 1500)

        captureThread = HandlerThread("CaptureLoop").apply { start() }
        captureHandler = Handler(captureThread!!.looper)

        imageReader!!.setOnImageAvailableListener({ reader ->
            val now = SystemClock.uptimeMillis()

        // throttle
            if (now - lastProcessMs < frameIntervalMs) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastProcessMs = now

        // Don't blur your own app
            if (App.inForeground) {
                overlay.hide()
                overlayShown = false
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }

            val img = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val fullBmp = img.toBitmap() ?: return@setOnImageAvailableListener
                val result = classifier?.classifyImage(fullBmp)

                if (result == null) {
                    android.util.Log.e("Guard", "Classifier returned null")
                    return@setOnImageAvailableListener
                }

                val threshold = getSharedPreferences("guard", MODE_PRIVATE)
                    .getFloat("threshold", 0.70f)

                val nudeConfidence = result.nudeConfidence

                // Log classification result for debugging
                android.util.Log.d(
                    "Guard",
                    "Classification: ${if (result.isNude) "NUDE" else "SAFE"} " +
                    "| Nude: ${(result.nudeConfidence * 100).toInt()}% " +
                    "| Safe: ${(result.safeConfidence * 100).toInt()}% " +
                    "| Time: ${result.processingTime}ms"
                )

                if (result.isNude && nudeConfidence >= threshold) {
                    android.util.Log.w("Guard", "⚠️ NUDITY DETECTED - Showing overlay")
                    overlay.show()
                    overlay.updateBlurredFrame(fullBmp)
                    overlayShown = true
                    lastHitMs = now

                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(1, buildNotif("⚠️ Inappropriate content detected!"))
                } else {
                    val keep = overlayShown && (now - lastHitMs < holdMs)
                    if (!keep) {
                        overlay.hide()
                        overlayShown = false

                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(1, buildNotif("Nudity Guard is running"))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Guard", "Error processing frame: ${e.message}")
            } finally {
                img.close()
            }
        }, captureHandler)
    }

    private fun registerScreenOffReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (Intent.ACTION_SCREEN_OFF == intent?.action) stopSelf()
            }
        }
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        try { overlay.hide() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { projectionCallback?.let { projection.unregisterCallback(it) } } catch (_: Exception) {}
        try { projection.stop() } catch (_: Exception) {}

        // Clean up the classifier
        try { classifier?.close() } catch (_: Exception) {}

        try {
            screenReceiver?.let { unregisterReceiver(it) }
            screenReceiver = null
        } catch (_: Exception) {}

        try {
            captureThread?.quitSafely()
            captureThread = null
            captureHandler = null
        } catch (_: Exception) {}

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotif(text: String): Notification {
        val chId = "guard"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    chId,
                    "Nudity Guard",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        return NotificationCompat.Builder(this, chId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Nudity Guard")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }
}
