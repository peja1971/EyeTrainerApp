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

class MainActivity : ComponentActivity() {
    private fun hideSystemBarsAlways() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        // BITNO: ne daj da ostanu vidljivi; ako user povuče, vrati se - mi ćemo ih opet sakriti
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBarsAlways()

// PATCH: boja sistema = boja aplikacije (da nema crnih traka)
        val appBg = Color.parseColor("#e8ecef")
        window.statusBarColor = appBg
        window.navigationBarColor = appBg

        val sys = WindowInsetsControllerCompat(window, window.decorView)
        sys.isAppearanceLightStatusBars = true
        sys.isAppearanceLightNavigationBars = true

        val webView = WebView(this)
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

        // ako hoces fullscreen iz JS-a
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        setContentView(webView)

        webView.loadUrl("file:///android_asset/eye_trainer.html")
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
        // Ako zelis hard cleanup:
        // (nije obavezno, ali moze)
    }
}
