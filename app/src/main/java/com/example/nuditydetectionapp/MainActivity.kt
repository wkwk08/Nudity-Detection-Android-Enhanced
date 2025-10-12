package com.example.nuditydetectionapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.media.projection.MediaProjectionManager

class MainActivity : AppCompatActivity() {

  private lateinit var toggle: Switch
  private val REQ_OVERLAY = 2001
  private val REQ_CAPTURE = 1001

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // (Optional) Android 13+ notification permission for foreground service
    if (Build.VERSION.SDK_INT >= 33) {
      // You can add a runtime request if you want; not strictly required to run
      // requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 3001)
    }

    toggle = findViewById(R.id.enableSwitch)
    toggle.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) enableGuard() else disableGuard()
    }
  }

  private fun enableGuard() {
    // 1) Overlay permission
    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
      )
      startActivityForResult(intent, REQ_OVERLAY)
      Toast.makeText(this, "Grant 'Display over other apps', then come back.", Toast.LENGTH_LONG).show()
      return
    }
    // 2) MediaProjection consent
    val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val captureIntent = mpm.createScreenCaptureIntent()
    startActivityForResult(captureIntent, REQ_CAPTURE)
  }

  private fun disableGuard() {
    stopService(Intent(this, ScreenGuardService::class.java))
    Toast.makeText(this, "Nudity Guard disabled", Toast.LENGTH_SHORT).show()
  }

  @Deprecated("Using legacy activity result for simplicity")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQ_OVERLAY -> {
        if (Settings.canDrawOverlays(this)) {
          enableGuard() // continue to capture flow
        } else {
          toggle.isChecked = false
          Toast.makeText(this, "Overlay permission required.", Toast.LENGTH_SHORT).show()
        }
      }
      REQ_CAPTURE -> {
        if (resultCode == Activity.RESULT_OK && data != null) {
          val svc = Intent(this, ScreenGuardService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("resultData", data)
          }
          ContextCompat.startForegroundService(this, svc)
          Toast.makeText(this, "Nudity Guard enabled", Toast.LENGTH_SHORT).show()
        } else {
          toggle.isChecked = false
          Toast.makeText(this, "Screen capture permission canceled.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}
