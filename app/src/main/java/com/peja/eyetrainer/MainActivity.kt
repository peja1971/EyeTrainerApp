package com.peja.eyetrainer

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.view.OrientationEventListener

class MainActivity : ComponentActivity() {

  private var tts: TextToSpeech? = null
  private var webView: WebView? = null
  private var orientationListener: OrientationEventListener? = null
  private var lastOrientationIsLandscape: Boolean? = null

  private fun hideSystemBarsAlways() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    hideSystemBarsAlways()

    tts = TextToSpeech(this) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.setSpeechRate(1.05f)
      }
    }

    val appBg = Color.parseColor("#e8ecef")
    window.statusBarColor = appBg
    window.navigationBarColor = appBg

    val sys = WindowInsetsControllerCompat(window, window.decorView)
    sys.isAppearanceLightStatusBars = true
    sys.isAppearanceLightNavigationBars = true

    val wv = WebView(this)
    webView = wv

    wv.addJavascriptInterface(TtsBridge(), "AndroidTTS")
    wv.addJavascriptInterface(ScreenControl(this), "AndroidScreen")
    wv.setBackgroundColor(appBg)
    wv.overScrollMode = View.OVER_SCROLL_NEVER

    wv.webViewClient = WebViewClient()
    wv.webChromeClient = WebChromeClient()

    wv.settings.javaScriptEnabled = true
    wv.settings.domStorageEnabled = true
    wv.settings.allowFileAccess = true
    wv.settings.allowContentAccess = true
    wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
    wv.settings.mediaPlaybackRequiresUserGesture = false
    wv.settings.javaScriptCanOpenWindowsAutomatically = true

    setContentView(wv)
    wv.loadUrl("file:///android_asset/eye_trainer.html")

    // OrientationEventListener radi i kad je auto-rotate ISKLJUČEN na telefonu.
    // Koristi direktno akcelerometar/žiroskop, nezavisno od system podešavanja.
    orientationListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
      override fun onOrientationChanged(orientation: Int) {
        if (orientation == ORIENTATION_UNKNOWN) return
        val isLandscape = (orientation in 60..120) || (orientation in 240..300)
        if (isLandscape != lastOrientationIsLandscape) {
          lastOrientationIsLandscape = isLandscape
          // Razlikuj landscape-left (90°) od landscape-right (270°)
          val orientationStr = when {
            orientation in 60..120  -> "landscape_right"
            orientation in 240..300 -> "landscape_left"
            else -> "portrait"
          }
          runOnUiThread {
            webView?.evaluateJavascript(
              "if(typeof onPhysicalOrientationChanged==='function') onPhysicalOrientationChanged('$orientationStr');",
              null
            )
          }
        }
      }
    }
    orientationListener?.enable()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) hideSystemBarsAlways()
  }

  override fun onResume() {
    super.onResume()
    hideSystemBarsAlways()
  }

  override fun onDestroy() {
    super.onDestroy()
    orientationListener?.disable()
    orientationListener = null
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

    @JavascriptInterface
    fun lockLandscape() {
      runOnUiThread {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
    }

    @JavascriptInterface
    fun unlockOrientation() {
      // Namerno prazno - landscape je uvek zaključan via Manifest
      // JS poziva ovo nakon stop, ali mi ne želimo da otključamo
    }
  }
}

class ScreenControl(private val activity: MainActivity) {
  @JavascriptInterface
  fun keepScreenOn(on: Boolean) {
    activity.runOnUiThread {
      if (on) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }
}