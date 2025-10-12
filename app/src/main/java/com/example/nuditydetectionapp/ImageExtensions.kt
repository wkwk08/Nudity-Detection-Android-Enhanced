package com.example.nuditydetectionapp

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image


fun Image.toBitmap(): Bitmap? {
    if (format != PixelFormat.RGBA_8888) return null
    val plane = planes[0]
    val buf = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * width
    val bmp = Bitmap.createBitmap(
        width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
    )
    bmp.copyPixelsFromBuffer(buf)
    return Bitmap.createBitmap(bmp, 0, 0, width, height)
}
