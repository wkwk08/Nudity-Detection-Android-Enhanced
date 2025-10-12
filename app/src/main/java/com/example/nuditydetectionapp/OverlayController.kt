package com.example.nuditydetectionapp

import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.*
import android.widget.ImageView

class OverlayController(private val ctx: Context) {
  private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private var overlay: View? = null
  private var imageView: ImageView? = null

  fun show() {
    if (overlay != null) return
    val iv = ImageView(ctx).apply {
      scaleType = ImageView.ScaleType.CENTER_CROP
      // subtle dark dim even when blurred/pixelated
      setBackgroundColor(Color.argb(110, 0, 0, 0))
    }
    imageView = iv
    overlay = iv

    val type = if (Build.VERSION.SDK_INT >= 26)
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else WindowManager.LayoutParams.TYPE_PHONE

    val lp = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      type,
      // not touchable/focusable so it doesn’t block user input
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
      PixelFormat.TRANSLUCENT
    )
    lp.gravity = Gravity.TOP or Gravity.START
    wm.addView(overlay, lp)
  }

  /** For API 31+ use real blur; older devices get pixelation fallback. */
  fun updateBlurredFrame(src: Bitmap?) {
    val iv = imageView ?: return
    if (src == null) return

    if (Build.VERSION.SDK_INT >= 31) {
      iv.setImageBitmap(src)
      iv.setRenderEffect(RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP))
      return
    }

    // Pixelation fallback (works on all APIs)
    val w = src.width.coerceAtLeast(1)
    val h = src.height.coerceAtLeast(1)
    val down = 16 // higher = more pixelated
    val small = Bitmap.createScaledBitmap(src, (w / down).coerceAtLeast(1), (h / down).coerceAtLeast(1), false)
    val pixelated = Bitmap.createScaledBitmap(small, w, h, false)

    val canvasBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(canvasBmp)
    c.drawBitmap(pixelated, 0f, 0f, null)

    // extra dimming to ensure content is obscured
    val p = Paint().apply { color = Color.argb(80, 0, 0, 0) }
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

    iv.setImageBitmap(canvasBmp)
  }

  fun hide() {
    overlay?.let { runCatching { wm.removeView(it) } }
    overlay = null
    imageView = null
  }
}
