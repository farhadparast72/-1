package com.example.proshell

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.proshell.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    private var currentView: String = "dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Navigation icon: open the web drawer
        binding.toolbar.setNavigationIcon(@android:drawable/ic_menu_sort_by_size)
        binding.toolbar.setNavigationOnClickListener {
            evalJs("if (window.openDrawer) { openDrawer(); } else { var b=document.getElementById('btnDrawer'); if(b) b.click(); }")
        }

        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = filePathCallback ?: return@registerForActivityResult
            filePathCallback = null

            if (result.resultCode != RESULT_OK || result.data == null) {
                callback.onReceiveValue(emptyArray())
                return@registerForActivityResult
            }

            val uri = result.data?.data
            if (uri != null) callback.onReceiveValue(arrayOf(uri))
            else callback.onReceiveValue(emptyArray())
        }

        setupWebView()
        setupUi()
    }

    private fun setupUi() {
        binding.swipe.setOnRefreshListener {
            binding.web.reload()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { go("dashboard"); true }
                R.id.nav_saved -> { go("saved"); true }
                R.id.nav_new -> { go("new"); true }
                R.id.nav_search -> { go("search"); true }
                R.id.nav_settings -> { go("settings"); true }
                else -> false
            }
        }

        binding.fab.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_new
            go("new")
        }

        // default
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
    }

    private fun setupWebView() {
        val wv = binding.web

        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.allowFileAccess = true
        wv.settings.allowContentAccess = true
        wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
        wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        wv.settings.setSupportZoom(false)

        CookieManager.getInstance().setAcceptCookie(true)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.swipe.isRefreshing = false
                injectNativeShell()
                // ensure current view is applied
                go(currentView)
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Cancel any existing callback
                this@MainActivity.filePathCallback?.onReceiveValue(emptyArray())
                this@MainActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null
                    Toast.makeText(this@MainActivity, "امکان انتخاب فایل نیست.", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        // Handle downloads (e.g., exports)
        wv.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val req = DownloadManager.Request(Uri.parse(url))
                req.setMimeType(mimeType)
                req.addRequestHeader("User-Agent", userAgent)
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                req.setAllowedOverMetered(true)
                req.setAllowedOverRoaming(true)
                req.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    guessFileName(contentDisposition, mimeType)
                )
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(req)
                Toast.makeText(this, "دانلود شروع شد…", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "دانلود ناموفق بود.", Toast.LENGTH_SHORT).show()
            }
        })

        wv.loadUrl("file:///android_asset/index.html")
    }

    private fun guessFileName(contentDisposition: String?, mimeType: String?): String {
        val cd = contentDisposition ?: ""
        val match = Regex("filename\\*=UTF-8''([^;]+)|filename=\\\"?([^;\\\"]+)\\\"?").find(cd)
        val name = match?.groups?.get(1)?.value ?: match?.groups?.get(2)?.value
        return name ?: ("download-" + System.currentTimeMillis() + (if (mimeType == "application/json") ".json" else ""))
    }

    private fun injectNativeShell() {
        // Hide web header + web bottom nav to avoid duplicate UI, keep it feeling "native"
        val js = """
            (function(){
              try{
                var h=document.querySelector('header'); if(h) h.style.display='none';
                var b=document.querySelector('.bottomNav'); if(b) b.style.display='none';
                document.body.style.paddingTop='0px';
                document.body.style.paddingBottom='0px';
              }catch(e){}
            })();
        """.trimIndent()
        binding.web.evaluateJavascript(js, null)
    }

    private fun go(name: String) {
        currentView = name
        updateTitle(name)
        evalJs("if (window.setActiveView) { setActiveView('${escapeJs(name)}'); }")
    }

    private fun updateTitle(name: String) {
        val t = when (name) {
            "dashboard" -> "داشبورد"
            "saved" -> "ذخیره‌ها"
            "new" -> "ثبت"
            "search" -> "جستجو"
            "settings" -> "تنظیمات"
            "contract" -> "قرارداد"
            else -> "نرم‌افزار"
        }
        supportActionBar?.title = t
        binding.toolbar.title = t
    }

    private fun evalJs(script: String) {
        binding.web.evaluateJavascript(script, null)
    }

    private fun escapeJs(s: String): String {
        return s.replace("\\\\", "\\\\\\\\").replace("'", "\\\\'")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                // click theme toggle in web UI if exists
                evalJs("var t=document.getElementById('btnTheme'); if(t) t.click(); else if(window.setTheme){ setTheme(document.documentElement.getAttribute('data-theme')==='dark'?'light':'dark'); }")
                true
            }
            R.id.action_refresh -> {
                binding.web.reload()
                true
            }
            android.R.id.home -> {
                evalJs("if (window.openDrawer) { openDrawer(); }")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        val wv = binding.web
        if (wv.canGoBack()) {
            wv.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
