package com.dosxdos.dosxdos.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.dosxdos.dosxdos.app.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Clase de binding generada automáticamente
    private val PERMISSIONS_REQUEST_CODE = 1001

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE, // Para Android 11
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filterValues { !it }.keys
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permisos denegados: $deniedPermissions", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Todos los permisos concedidos correctamente", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Establece el layout con el binding

        requestPermissions() // 🔹 Solicitar permisos al iniciar la app

        logica()

        // Restaurar el estado del WebView si hay uno guardado
        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            binding.webView.loadUrl("https://dosxdos.app.iidos.com/")
        }
    }


    private fun logica() {
        val webView = binding.webView
        val webSettings = webView.settings

        // 🔹 Habilitar JavaScript y almacenamiento local
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        // 🔹 Configurar caché correctamente
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK // Usa caché si no hay internet

        // 🔹 Habilitar acceso a archivos y contenido local
        webSettings.allowContentAccess = true
        webSettings.allowFileAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true

        // 🔹 Habilitar geolocalización
        webSettings.setGeolocationEnabled(true)

        // 🔹 Configurar almacenamiento de caché para imágenes
        webSettings.loadsImagesAutomatically = true
        webSettings.mediaPlaybackRequiresUserGesture = false

        // 🔹 Habilitar depuración de WebView
        WebView.setWebContentsDebuggingEnabled(true)

        // 🔹 Interceptar solicitudes y gestionar caché
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                val cacheFile = getCachedFile(url)

                return if (!isNetworkAvailable() && cacheFile.exists()) {
                    Log.d("WebView", "Cargando desde caché: ${cacheFile.absolutePath}")
                    getCachedWebResource(cacheFile, url)
                } else {
                    super.shouldInterceptRequest(view, request)
                    //Actualizar la url en la cache
                    overWriteUrl(cacheFile, url)
                }
            }


            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

                val url = request?.url.toString()
                val cacheFile = getCachedFile(url)

                if (cacheFile.exists()) {
                    Log.d("WebView", "Cargando desde caché: ${cacheFile.absolutePath}")
                    view?.loadUrl("file://${cacheFile.absolutePath}")
                } else {
                    Log.e("WebView", "Archivo no encontrado en caché. No se puede cargar sin conexión.")
                }
            }
        }

        // 🔹 Configurar permisos en WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                callback?.invoke(origin, true, false)
            }
        }

        // 🔹 Cargar la URL principal
        webView.loadUrl("https://dosxdos.app.iidos.com/")
    }

    private fun overWriteUrl(file: File, url: String): WebResourceResponse? {
        return try {
            // Si el archivo ya existe, lo eliminamos antes de descargar el nuevo contenido
            if (file.exists()) {
                file.delete()
                Log.d("WebView", "Archivo en caché reemplazado: ${file.absolutePath}")
            }

            // Crear una nueva conexión a la URL
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(file)

                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                // Devolver el archivo actualizado como WebResourceResponse
                Log.d("WebView", "Archivo guardado y reemplazado en caché: ${file.absolutePath}")

                // Cargar el archivo de nuevo como respuesta
                return WebResourceResponse("text/html", "UTF-8", FileInputStream(file))
            } else {
                Log.e("WebView", "Error al descargar el archivo: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun getCachedFile(url: String): File {
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment ?: "index.html" // Si no hay nombre, usa index.html
        return File(filesDir, fileName) // 📌 Se usa `filesDir` en lugar de `cacheDir`
    }


    private fun getCachedWebResource(file: File, url: String): WebResourceResponse? {
        return try {
            val mimeType = getMimeType(url)
            WebResourceResponse(mimeType, "UTF-8", FileInputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMimeType(url: String): String {
        return when {
            url.endsWith(".html") -> "text/html"
            url.endsWith(".css") -> "text/css"
            url.endsWith(".js") -> "application/javascript"
            url.endsWith(".json") -> "application/json"
            url.endsWith(".png") -> "image/png"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".svg") -> "image/svg+xml"
            url.endsWith(".webp") -> "image/webp"
            url.endsWith(".gif") -> "image/gif"
            else -> "text/html"
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }


    private fun requestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toMutableList()

        // 🔹 Si es Android 11+ y falta MANAGE_EXTERNAL_STORAGE, redirigir manualmente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                missingPermissions.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                showStoragePermissionDialog()
            }
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun showStoragePermissionDialog() {
        Toast.makeText(this, "Necesitas permitir acceso total a archivos", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }



    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Si hay historial, navega hacia atrás en lugar de cerrar la actividad
        val webView = binding.webView
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Guardar estado del WebView cuando la actividad se destruye
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    // Restaurar estado del WebView cuando la actividad se recrea
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }
}