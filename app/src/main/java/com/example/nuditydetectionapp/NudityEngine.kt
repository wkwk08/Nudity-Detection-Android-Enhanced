package com.example.nuditydetectionapp

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max

class NudityEngine {
  /**
   * Returns a score in [0..1] = fraction of pixels that look like skin.
   * You’ll tune the threshold (e.g., 0.60) in ScreenGuardService.
   */
  fun score(bmp: Bitmap): Float {
    val w = bmp.width; val h = bmp.height
    if (w <= 0 || h <= 0) return 0f
    val total = w * h
    val pixels = IntArray(total)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)

    var skin = 0
    for (p in pixels) {
      val r = (p shr 16) and 0xFF
      val g = (p shr 8) and 0xFF
      val b = p and 0xFF
      if (isSkin(r, g, b)) skin++
    }
    return skin / max(1f, total.toFloat())
  }

  /** A widely-used quick skin detector using RGB + HSV gates. */
  private fun isSkin(r: Int, g: Int, b: Int): Boolean {
    // Basic RGB rule
    val rgbRule = (r > 95 && g > 40 && b > 20 &&
                   abs(r - g) > 15 && r > g && r > b)
    // HSV rule improves robustness across lighting
    val hsv = FloatArray(3)
    Color.RGBToHSV(r, g, b, hsv)
    val h = hsv[0]; val s = hsv[1]; val v = hsv[2]
    val hsvRule = (h in 0f..50f && s in 0.23f..0.78f && v > 0.35f)
    return rgbRule && hsvRule
  }
}
