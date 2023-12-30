package com.srs.weather.ui.view

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.srs.weather.R
import com.srs.weather.utils.PrefManager


class WebView : AppCompatActivity() {
    private lateinit var webView: WebView
    private val urlDashboardAWS = "https://iot.srs-ssms.com/"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webviewQC)

        // Enable file access
        webView.settings.allowFileAccess = true
        // Set the User-Agent string
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
        // Enable file downloads
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        //webView.settings.setAppCachePath(cacheDir.path)
        webView.settings.javaScriptEnabled = true
        //webView.settings.setAppCacheEnabled(true)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.loadUrl(urlDashboardAWS) // Load your desired URL

        // Enable downloads
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            //------------------------COOKIE!!------------------------
            val cookies: String? = CookieManager.getInstance().getCookie(url)
            if(cookies != null) {
                request.addRequestHeader("cookie", cookies)
            }
            //------------------------COOKIE!!------------------------
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimetype)
            )
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
        }
        setupWebView()
    }

    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                autoLogin()
            }
        }
    }

    private fun autoLogin() {
        val email = PrefManager(this).email!!
        val password = PrefManager(this).password!!

        val js = "javascript:(function(){" +
                "document.getElementById('username').value = '$email';" +
                "document.getElementById('password').value = '$password';" +
                "document.getElementById('loginButton').click();" +
                "})()"
        webView.evaluateJavascript(js) {}
    }

    // Handle the back button press event to navigate back in the WebView history
    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
        }
    }
}