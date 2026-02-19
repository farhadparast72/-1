package com.example.amlakultra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // UI نیتیو فارسی: RTL پیش‌فرض
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    AmlakShell()
                }
            }
        }
    }
}

private data class NavItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun AmlakShell() {
    val context = LocalContext.current

    val items = remember {
        listOf(
            NavItem("saved", "ذخیره‌شده", Icons.Filled.Save),
            NavItem("dashboard", "داشبورد", Icons.Filled.Home),
            NavItem("new", "ثبت", Icons.Filled.Add),
            NavItem("search", "جستجو", Icons.Filled.Search),
            NavItem("contract", "قرارداد", Icons.Filled.Description),
            NavItem("settings", "تنظیمات", Icons.Filled.Settings),
        )
    }

    var selected by remember { mutableStateOf(items[1]) } // dashboard
    var refreshing by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            webViewRef?.reload()
        }
    )

    fun jsCall(script: String) {
        webViewRef?.evaluateJavascript(script, null)
    }

    fun goTo(viewId: String) {
        jsCall("try { setActiveView('$viewId'); } catch(e) { console.log(e); }")
    }

    fun openDrawer() {
        jsCall("try { openDrawer(); } catch(e) { console.log(e); }")
    }

    fun injectHideWebChrome() {
        val injection =
            "(function(){try{" +
                "var h=document.querySelector('header'); if(h) h.style.display='none';" +
                "var b=document.getElementById('bottomNav'); if(b) b.style.display='none';" +
                "document.body.style.paddingTop='0px'; document.body.style.paddingBottom='0px';" +
            "}catch(e){}})();"
        jsCall(injection)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected.label) },
                navigationIcon = {
                    IconButton(onClick = { openDrawer() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        refreshing = true
                        webViewRef?.reload()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selected = items.first { it.id == "new" }
                    goTo("new")
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "ثبت")
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = item.id == selected.id,
                        onClick = {
                            selected = item
                            goTo(item.id)
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .pullRefresh(pullRefreshState)
        ) {
            val assetLoader = remember(context) {
                WebViewAssetLoader.Builder()
                    .addPathHandler(
                        "/assets/",
                        WebViewAssetLoader.AssetsPathHandler(context)
                    )
                    .build()
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true

                        addJavascriptInterface(AndroidBridge(ctx), "AndroidBridge")

                        webViewClient = object : WebViewClient() {

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url.toString()

                                // tel:/mailto: -> external intent
                                if (url.startsWith("tel:") || url.startsWith("mailto:")) {
                                    try {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (_: Exception) { }
                                    return true
                                }

                                // appassets -> allow WebViewAssetLoader
                                if (assetLoader.shouldInterceptRequest(request.url) != null) {
                                    return false
                                }

                                // Any external links -> open browser
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    return try {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        true
                                    } catch (_: Exception) { false }
                                }

                                return false
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                refreshing = false
                                injectHideWebChrome()
                                goTo(selected.id)
                            }
                        }

                        // Stable origin (better for localStorage) via WebViewAssetLoader
                        loadUrl("https://appassets.androidplatform.net/assets/app/index.html")
                        webViewRef = this
                    }
                },
                update = { webViewRef = it }
            )

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }
}

private class AndroidBridge(private val context: android.content.Context) {
    @JavascriptInterface
    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
