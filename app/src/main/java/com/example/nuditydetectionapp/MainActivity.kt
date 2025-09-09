/**
 * Nudity-Detection-Android
 * Original implementation by: Rahat Yeasin Emon
 * GitHub: https://github.com/rahatyeasinemon/Nudity-Detection-Android
 * Research Paper: https://arxiv.org/ftp/arxiv/papers/2006/2006.01780.pdf
 */

package com.example.nuditydetectionapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.example.nuditydetectionapp.R

class MainActivity : AppCompatActivity() {

    private var facePixel = 0
    private var skinPixel = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.imageView)
        checkNudity(imageView)
    }

    private fun classifySkin(r: Int, g: Int, b: Int): Boolean {
        val rgbClassifier = (r > 95) && (g > 40) && (b > 20) &&
                (Math.abs(r - g) > 15) && (r > g) && (r > b)

        val hsv = FloatArray(3)
        val currentColor = Color.rgb(r, g, b)
        Color.colorToHSV(currentColor, hsv)
        val h = hsv[0]
        val s = hsv[1]
        val hsvClassifier = (h >= 0 && h <= 50 && s >= 0.23 && s <= 0.78)

        return rgbClassifier && hsvClassifier
    }

    private fun checkNudity(imageView: ImageView) {
        var cnt = 0 // Unused in original code, kept for fidelity

        // Get image bitmap from ImageView
        val bitmapImage = (imageView.drawable as? BitmapDrawable)?.bitmap
        if (bitmapImage == null) {
            AlertDialog.Builder(this)
                .setMessage("No valid image found in ImageView")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Resize large image to speed up detection
        val resizedBitmap = if (bitmapImage.width >= bitmapImage.height && bitmapImage.width > 1000) {
            val th = bitmapImage.height.toFloat() / bitmapImage.width
            Bitmap.createScaledBitmap(bitmapImage, 1000, (1000 * th).toInt(), true)
        } else if (bitmapImage.height > bitmapImage.width && bitmapImage.height > 1000) {
            val th = bitmapImage.width.toFloat() / bitmapImage.height
            Bitmap.createScaledBitmap(bitmapImage, (1000 * th).toInt(), 1000, true)
        } else {
            bitmapImage
        }

        val bm = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        var t = 0

        val tempBitmap = Bitmap.createBitmap(resizedBitmap.width, resizedBitmap.height, Bitmap.Config.RGB_565)
        val tempCanvas = Canvas(tempBitmap)
        tempCanvas.drawBitmap(resizedBitmap, 0f, 0f, null)

        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()

        if (!faceDetector.isOperational()) {
            AlertDialog.Builder(this)
                .setMessage("Could not set up the face detector!")
                .setPositiveButton("OK", null)
                .show()
            faceDetector.release()
            return
        }

        val frame = Frame.Builder().setBitmap(resizedBitmap).build()
        val faces = faceDetector.detect(frame)

        for (i in 0 until faces.size()) {
            val thisFace = faces.valueAt(i)
            val x1 = thisFace.position.x.toInt().coerceAtLeast(0)
            val y1 = thisFace.position.y.toInt().coerceAtLeast(0)
            val x2 = (x1 + thisFace.width.toInt()).coerceAtMost(resizedBitmap.width - 1)
            val y2 = (y1 + thisFace.height.toInt()).coerceAtMost(resizedBitmap.height - 1)

            for (i1 in x1 until x2 + 1) {
                for (j1 in y1 until y2 + 1) {
                    val pixel = resizedBitmap.getPixel(i1, j1)
                    val r = Color.red(pixel)
                    val b = Color.blue(pixel)
                    val g = Color.green(pixel)

                    if (classifySkin(r, g, b)) {
                        facePixel++
                    }
                }
            }
        }

        // Fix: Create a mutable copy of tempBitmap for pixel manipulation
        val mutableBm = tempBitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (x in 0 until resizedBitmap.width) {
            for (y in 0 until resizedBitmap.height) {
                val pixel = resizedBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val b = Color.blue(pixel)
                val g = Color.green(pixel)

                if (classifySkin(r, g, b)) {
                    skinPixel++
                    mutableBm.setPixel(x, y, Color.rgb((t + 101) % 255, (10 + t) % 255, (110 + t) % 255))
                }
            }
        }

        imageView.setImageBitmap(mutableBm)
        println("total skin pixel in face $facePixel")
        println("total skin pixel in overall image $skinPixel")

        faceDetector.release()
    }
}