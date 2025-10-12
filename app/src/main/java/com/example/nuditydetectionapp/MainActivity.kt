package com.example.nuditydetectionapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class MainActivity : AppCompatActivity() {

    private val REQ_OVERLAY = 2001
    private val REQ_CAPTURE = 1001
    private var updateToggleState: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NudityDetectionApp(
                onEnableGuard = { enableGuard() },
                onDisableGuard = { disableGuard() },
                onStateCallback = { updateToggleState = it }
            )
        }

        if (Build.VERSION.SDK_INT >= 33) {
            // requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 3001)
        }
    }

    private fun enableGuard() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQ_OVERLAY)
            Toast.makeText(this, "Grant 'Display over other apps', then come back.", Toast.LENGTH_LONG).show()
            return
        }
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
                    enableGuard()
                } else {
                    updateToggleState?.invoke(false)
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
                    updateToggleState?.invoke(false)
                    Toast.makeText(this, "Screen capture permission canceled.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun NudityDetectionApp(
    onEnableGuard: () -> Unit,
    onDisableGuard: () -> Unit,
    onStateCallback: ((Boolean) -> Unit) -> Unit
) {
    var currentScreen by remember { mutableStateOf("welcome") }
    var isSafetyModeEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onStateCallback { enabled ->
            isSafetyModeEnabled = enabled
        }
    }

    when (currentScreen) {
        "welcome" -> WelcomeScreen(
            onStartClick = { currentScreen = "detection" }
        )
        "detection" -> DetectionScreen(
            isSafetyModeEnabled = isSafetyModeEnabled,
            onToggleSafetyMode = {
                isSafetyModeEnabled = it
                if (it) {
                    onEnableGuard()
                } else {
                    onDisableGuard()
                }
            }
        )
    }
}

@Composable
fun WelcomeScreen(onStartClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F6))
    ) {
        // Top-right blob image
        Image(
            painter = painterResource(id = R.drawable.top_right_border),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 150.dp, height = 149.dp)
        )

        // Bottom/mid-left blob image
        Image(
            painter = painterResource(id = R.drawable.mid_left_border),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = (-300).dp)
                .size(width = 300.dp, height = 150.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.sensivue_logo),
                contentDescription = "Sensivue Logo",
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Sensivue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Welcome card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFF8A80),
                        shape = RoundedCornerShape(
                            topStart = 40.dp,
                            topEnd = 40.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome! This app helps ensure safety by detecting and filtering inappropriate or explicit images with advanced AI technology.",
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Rounded start button as in Figma
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .height(48.dp)
                        .width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF933C3C)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Start",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DetectionScreen(
    isSafetyModeEnabled: Boolean,
    onToggleSafetyMode: (Boolean) -> Unit
) {
    val backgroundColor = if (isSafetyModeEnabled) {
        Color(0xFFB8E6BD)
    } else {
        Color(0xFFE8B5A0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(
                    id = if (isSafetyModeEnabled) {
                        R.drawable.to_on_toggle
                    } else {
                        R.drawable.to_off_toggle
                    }
                ),
                contentDescription = "Toggle Status",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "NUDITY DETECTION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSafetyModeEnabled) {
                    "Nudity filter is ON — keeping your space safe."
                } else {
                    "Safety mode off — nudity detection is not active."
                },
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = if (isSafetyModeEnabled) {
                            Color(0xFF66BB6A)
                        } else {
                            Color(0xFFBFAEAE)
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                    .height(48.dp)
                    .width(100.dp)
                    .clickable {
                        onToggleSafetyMode(!isSafetyModeEnabled)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSafetyModeEnabled) "ON" else "OFF",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isSafetyModeEnabled) {
                                    Color(0xFF4CAF50)
                                } else {
                                    Color(0xFFD32F2F)
                                },
                                shape = RoundedCornerShape(50.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}