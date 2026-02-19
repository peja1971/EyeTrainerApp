package com.peja.eyetrainer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

  private var tts: TextToSpeech? = null

  // Runtime-only state (NE cuva se preko restarta app)
  @Volatile var rotationLocked: Boolean = false

  // Koristimo da privremeno zakljucamo orijentaciju dok otvaramo Settings,
  // pa da vratimo prethodni rezim kad se vratimo u app.
  @Volatile var pendingRestoreOrientation: Int? = null

  private fun hideSystemBarsAlways() {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    controller.hide(WindowInsetsCompat.Type.systemBars())
  }

  // Postuje sistemski auto-rotate:
  // - ako je auto-rotate OFF, USER ce ostati fiksiran
  // - ako je auto-rotate ON, USER ce rotirati
  private fun applyRotationPolicy() {
    requestedOrientation = if (rotationLocked) {
      ActivityInfo.SCREEN_ORIENTATION_LOCKED
    } else {
      ActivityInfo.SCREEN_ORIENTATION_USER
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Default: ne forsiramo FULL_SENSOR, vec USER (postuje sistemski auto-rotate)
    rotationLocked = false
    applyRotationPolicy()

    hideSystemBarsAlways()

    tts = TextToSpeech(this) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.setSpeechRate(1.05f)
      }
    }

    // boja sistema = boja aplikacije (da nema crnih traka)
    val appBg = Color.parseColor("#e8ecef")
    window.statusBarColor = appBg
    window.navigationBarColor = appBg

    val sys = WindowInsetsControllerCompat(window, window.decorView)
    sys.isAppearanceLightStatusBars = true
    sys.isAppearanceLightNavigationBars = true

    val webView = WebView(this)
    webView.addJavascriptInterface(TtsBridge(), "AndroidTTS")
    webView.addJavascriptInterface(ScreenControl(this), "AndroidScreen")

    webView.setBackgroundColor(appBg)
    webView.overScrollMode = View.OVER_SCROLL_NEVER

    webView.webViewClient = WebViewClient()
    webView.webChromeClient = WebChromeClient()

    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.settings.allowFileAccess = true
    webView.settings.allowContentAccess = true
    webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
    webView.settings.mediaPlaybackRequiresUserGesture = false
    webView.settings.javaScriptCanOpenWindowsAutomatically = true

    setContentView(webView)
    webView.loadUrl("file:///android_asset/eye_trainer.html")
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      hideSystemBarsAlways()
      // kad se vratimo iz Settings, vrati prethodni rezim
      restoreOrientationIfNeeded()
    }
  }

  override fun onResume() {
    super.onResume()
    hideSystemBarsAlways()
    restoreOrientationIfNeeded()
  }

  private fun restoreOrientationIfNeeded() {
    val restore = pendingRestoreOrientation
    if (restore != null) {
      pendingRestoreOrientation = null
      requestedOrientation = restore
    } else {
      // uvek drzi konzistentno (USER ili LOCKED)
      applyRotationPolicy()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    tts?.stop()
    tts?.shutdown()
    tts = null
  }

  inner class TtsBridge {
    @JavascriptInterface
    fun speak(text: String) {
      runOnUiThread {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eyetrainer_tts")
      }
    }
  }
}

class ScreenControl(private val activity: MainActivity) {

  @JavascriptInterface
  fun lockRotation() {
    activity.runOnUiThread {
      activity.rotationLocked = true
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
      activity.window.decorView.postDelayed({
        activity.window.decorView.requestLayout()
      }, 100)
    }
  }

  @JavascriptInterface
  fun unlockRotation() {
    activity.runOnUiThread {
      activity.rotationLocked = false
      // BITNO: USER (ne FULL_SENSOR) da postuje sistemski auto-rotate
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
      activity.window.decorView.postDelayed({
        activity.window.decorView.requestLayout()
      }, 100)
    }
  }

  @JavascriptInterface
  fun keepScreenOn(on: Boolean) {
    activity.runOnUiThread {
      if (on) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  @JavascriptInterface
  fun openAutoRotateSettings() {
    activity.runOnUiThread {
      // SACUVAJ trenutno stanje, da ga vratimo kad se vratimo u app
      val before = activity.requestedOrientation
      activity.pendingRestoreOrientation = before

      // Privremeno zakljucaj TRENUTNU orijentaciju (ne menja portrait/landscape),
      // samo sprecava da Settings "nasledi" neki agresivan sensor rezim.
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

      val intents = listOf(
        Intent("android.settings.AUTO_ROTATE_SETTINGS"), // neki OEM-ovi
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        Intent(Settings.ACTION_DISPLAY_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
      )

      for (i in intents) {
        try {
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          activity.startActivity(i)
          return@runOnUiThread
        } catch (_: Exception) {
          // probaj sledeci
        }
      }

      // Ako nijedan intent ne prodje, vrati orijentaciju odmah
      activity.pendingRestoreOrientation = null
      activity.requestedOrientation = before
    }
  }
}
