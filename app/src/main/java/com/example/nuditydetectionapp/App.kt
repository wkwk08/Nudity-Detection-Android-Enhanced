package com.example.nuditydetectionapp

import android.app.Activity
import android.app.Application
import android.os.Bundle

class App : Application() {
  companion object { @Volatile var inForeground: Boolean = false }

  override fun onCreate() {
    super.onCreate()
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityStarted(activity: Activity) { inForeground = true }
      override fun onActivityStopped(activity: Activity)  { inForeground = false }
      override fun onActivityCreated(a: Activity, b: Bundle?) {}
      override fun onActivityResumed(a: Activity) {}
      override fun onActivityPaused(a: Activity) {}
      override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(a: Activity) {}
    })
  }
}
