package com.dosxdos.dosxdos.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dosxdos.dosxdos.app.Activities.ErrorPermisos
import com.dosxdos.dosxdos.app.Nativo.Notificaciones
import com.dosxdos.dosxdos.app.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.dosxdos.dosxdos.app.notifications.NotificationReceiver
import com.dosxdos.dosxdos.app.permissions.PermissionHelper
import com.dosxdos.dosxdos.app.webview.FileChooserHelper
import com.dosxdos.dosxdos.app.webview.WebViewHelper
import android.app.Activity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val FILE_CHOOSER_REQUEST_CODE = 1002
    private lateinit var firebaseTokenManager: Notificaciones
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var notificationReceiver: NotificationReceiver

    private val REQUIRED_PERMISSIONS = PermissionHelper.getRequiredPermissions()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filterValues { !it }.keys
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permisos denegados: $deniedPermissions", Toast.LENGTH_LONG).show()
                val intent = Intent(this, ErrorPermisos::class.java)
                startActivity(intent)
                finish()
            } else {
                initWebView()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationReceiver = NotificationReceiver { message ->
                injectMessageIntoWebView(message)
            }
            val filter = android.content.IntentFilter("com.dosxdos.dosxdos.FIREBASE_MESSAGE")
            registerReceiver(notificationReceiver, filter, RECEIVER_EXPORTED)
        }

        val url = intent.getStringExtra("url")

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        requestPermissions()
        if (PermissionHelper.arePermissionsGranted(this, REQUIRED_PERMISSIONS)) {
            initWebView(url ?: "https://dosxdos.app.iidos.com/")
        }
    }

    private fun requestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun injectMessageIntoWebView(message: String?) {
        val script = """
            javascript:(function() {
                window.localStorage.setItem('dataNotificacionNativa', '$message');
                console.log('Mensaje guardado en localStorage: ' + '$message');
                notificarWebApp()
            })()
        """
        binding.webView.evaluateJavascript(script) { result ->
            android.util.Log.d("WebView", "Resultado del script: $result")
        }
    }

    private fun initWebView(url: String = "https://dosxdos.app.iidos.com/") {
        WebViewHelper.configureWebView(
            context = this,
            webView = binding.webView,
            swipeRefreshEnabled = { isEnabled -> binding.swipeRefreshLayout.isEnabled = isEnabled },
            onTokenReady = { tokenPageUrl ->
                firebaseTokenManager = Notificaciones(this)
                firebaseTokenManager.getStoredToken { token ->
                    if (!token.isNullOrEmpty() && tokenPageUrl == "https://dosxdos.app.iidos.com/index.html") {
                        val script = """
                            javascript:(function() {
                                window.localStorage.setItem('tokenNativo', '$token');
                                console.log('✅ Token inyectado desde Android');
                                asignarTokenNativo()
                            })()
                        """
                        binding.webView.loadUrl(script)
                    }
                }
            },
            onInjectJS = { view, currentUrl ->
                if (currentUrl == "https://dosxdos.app.iidos.com/linea_montador.html") {
                    WebViewHelper.injectFileInputHandler(view)
                    WebViewHelper.injectFilesFunction(view)
                }
            },
            onOfflineFallback = { view, path -> view?.loadUrl("file://$path") },
            openFileChooserCallback = { openFileChooser() },
            onFileChooserIntent = { callback, intent ->
                mFilePathCallback = callback
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            }
        )
        binding.webView.loadUrl(url)
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
        } catch (e: Exception) {
            mFilePathCallback?.onReceiveValue(null)
            mFilePathCallback = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val results = FileChooserHelper.getResultUri(data)
            if (results != null) {
                val fileObjects = FileChooserHelper.convertUrisToFileData(contentResolver, results)
                val jsonFiles = Gson().toJson(fileObjects)
                binding.webView.evaluateJavascript("injectFilesToInput($jsonFiles);", null)
            } else {
                mFilePathCallback?.onReceiveValue(null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::notificationReceiver.isInitialized) unregisterReceiver(notificationReceiver)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        mFilePathCallback?.onReceiveValue(null)
        mFilePathCallback = null

        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
