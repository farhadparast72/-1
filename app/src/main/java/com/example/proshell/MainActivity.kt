package com.example.proshell

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.proshell.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val cb = fileCallback ?: return@registerForActivityResult
        fileCallback = null

        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {
            }
            cb.onReceiveValue(arrayOf(uri))
        } else {
            cb.onReceiveValue(null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.swipe.setOnRefreshListener { binding.web.reload() }

        binding.web.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.swipe.isRefreshing = false

                    // Hide HTML header (we already have native UI)
                    evaluateJavascript(
                        """
                        (function(){
                          try{
                            var h=document.querySelector('header');
                            if(h){ h.style.display='none'; }
                            document.body.style.paddingTop='0';
                          }catch(e){}
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = filePathCallback
                    pickFile.launch(arrayOf("application/json", "*/*"))
                    return true
                }
            }

            loadUrl("file:///android_asset/index.html")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tx -> { clickId("tabTx"); true }
                R.id.nav_debt -> { clickId("tabDebt"); true }
                R.id.nav_backup -> { clickId("btnBackup"); true }
                else -> false
            }
        }

        binding.fab.setOnClickListener { clickId("btnAddMain") }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> { clickId("btnBackup"); true }
            R.id.action_restore -> { clickId("btnRestore"); true }
            R.id.action_reset -> { clickId("btnReset"); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clickId(id: String) {
        binding.web.evaluateJavascript(
            "(function(){var el=document.getElementById('$id'); if(el) el.click();})()",
            null
        )
    }

    override fun onBackPressed() {
        if (binding.web.canGoBack()) binding.web.goBack() else super.onBackPressed()
    }
}
