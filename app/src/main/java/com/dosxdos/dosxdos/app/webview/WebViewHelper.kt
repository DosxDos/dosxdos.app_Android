package com.dosxdos.dosxdos.app.webview

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.webkit.*

object WebViewHelper {

    fun configureWebView(
        context: Context,
        webView: WebView,
        swipeRefreshEnabled: (Boolean) -> Unit,
        onTokenReady: (String) -> Unit,
        onInjectJS: (WebView?, String?) -> Unit,
        onOfflineFallback: (WebView?, String) -> Unit,
        openFileChooserCallback: () -> Unit,
        onFileChooserIntent: (ValueCallback<Array<Uri>>?, Intent) -> Unit
    ) {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.allowContentAccess = true
        webSettings.allowFileAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.setGeolocationEnabled(true)
        webSettings.loadsImagesAutomatically = true
        webSettings.mediaPlaybackRequiresUserGesture = false

        webView.addJavascriptInterface(JSInterface(openFileChooserCallback), "Android")

        webView.webViewClient = object : WebViewClient() {
            private val RUTAS_SIN_SWIPE = setOf(
                "https://dosxdos.app.iidos.com/linea_montador.html",
                "https://dosxdos.app.iidos.com/mapa_ruta.html",
                "https://dosxdos.app.iidos.com/mapa_ruta_historial.html"
            )

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && RUTAS_SIN_SWIPE.contains(url)) {
                    swipeRefreshEnabled(false)
                } else {
                    swipeRefreshEnabled(true)
                }

                return if (url?.contains("dosxdos.app.iidos.com") == true) {
                    false
                } else {
                    if (!isNetworkAvailable(context)) {
                        return true
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                        return true
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onInjectJS(view, url)
                if (url == "https://dosxdos.app.iidos.com/index.html") {
                    onTokenReady(url)
                }
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                val file = WebViewCacheManager.getCachedFile(context, url)

                return if (!isNetworkAvailable(context)) {
                    if (file.exists()) {
                        android.util.Log.d("WebView", "📁 Sin conexión: cargando ${file.name} desde caché")
                        WebViewCacheManager.getCachedWebResource(file, url)
                    } else {
                        android.util.Log.w("WebView", "❌ Archivo no encontrado en caché para $url")
                        null
                    }
                } else {
                    // ⚠️ NO devolvemos nada: dejamos que WebView use red
                    Thread {
                        WebViewCacheManager.overWriteUrl(file, url)
                    }.start()
                    null
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                val url = request?.url.toString()
                val file = WebViewCacheManager.getCachedFile(context, url)

                if (!isNetworkAvailable(context) && file.exists()) {
                    onOfflineFallback(view, file.absolutePath)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                val intent = fileChooserParams?.createIntent()
                if (intent != null) {
                    onFileChooserIntent(filePathCallback, intent)
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
                return true
            }
        }
    }

    fun injectFileInputHandler(webView: WebView?) {
        webView?.evaluateJavascript("""
        (function() {
            var input = document.querySelector('input[type="file"]');
            if (input) {
                input.addEventListener('click', function(event) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (!input.hasAttribute('data-clicked')) {
                        input.setAttribute('data-clicked', 'true');
                        Android.openFileChooser();
                        setTimeout(function() {
                            input.removeAttribute('data-clicked');
                        }, 1000);
                    }
                });
            }
        })();
        """.trimIndent(), null)
    }

    fun injectFilesFunction(webView: WebView?) {
        val jsCode = """
        function injectFilesToInput(files) {
            var input = document.querySelector('input[type="file"]');
            if (!input) return;
            var dataTransfer = new DataTransfer();
            files.forEach(function(file) {
                var byteCharacters = atob(file.base64Data);
                var byteArrays = [];
                for (var offset = 0; offset < byteCharacters.length; offset += 512) {
                    var slice = byteCharacters.slice(offset, offset + 512);
                    var byteNumbers = new Array(slice.length);
                    for (var i = 0; i < slice.length; i++) {
                        byteNumbers[i] = slice.charCodeAt(i);
                    }
                    byteArrays.push(new Uint8Array(byteNumbers));
                }
                var blob = new Blob(byteArrays, { type: file.type });
                var newFile = new File([blob], file.name, { type: file.type });
                dataTransfer.items.add(newFile);
            });
            input.files = dataTransfer.files;
            var event = new Event('change', { bubbles: true });
            input.dispatchEvent(event);
            localStorage.setItem('noResetForm', 'true');
        }
        """
        webView?.evaluateJavascript(jsCode.trimIndent(), null)
    }

    class JSInterface(private val openFileChooserCallback: () -> Unit) {
        @JavascriptInterface
        fun openFileChooser() {
            openFileChooserCallback()
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }

}