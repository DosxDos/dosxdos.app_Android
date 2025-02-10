package com.dosxdos.dosxdos.app

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dosxdos.dosxdos.app.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Clase de binding generada automáticamente

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Establece el layout con el binding

        logica()
    }


    private fun logica() {
        // Configuración básica del WebView usando el binding
        val webView = binding.webView // Accede al WebView desde el binding

        val webSettings = webView.settings

        // Habilitar JavaScript (muy importante para muchas aplicaciones web modernas)
        webSettings.javaScriptEnabled = true

        // Habilitar cookies (para manejar sesiones, por ejemplo)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // Habilitar almacenamiento local (LocalStorage)
        webSettings.domStorageEnabled = true

        // Habilitar almacenamiento de base de datos (IndexedDB)
        webSettings.databaseEnabled = true

        // Habilitar almacenamiento en caché
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT)

        // Permitir la geolocalización (si la página la requiere)
        webSettings.setGeolocationEnabled(true)

        // Habilitar la depuración de WebView (útil para ver errores en consola)
        WebView.setWebContentsDebuggingEnabled(true)

        // Habilitar ejecución de scripts en segundo plano
        webSettings.setAllowContentAccess(true)
        webSettings.setAllowFileAccess(true)

        // Permitir que el WebView acceda a archivos
        webSettings.allowFileAccess = true

        // Establecer el WebViewClient para manejar la carga de enlaces dentro del WebView
        webView.webViewClient = WebViewClient()

        // Cargar una URL (en este caso, la URL de tu página)
        webView.loadUrl("https://dosxdos.app.iidos.com/")
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
}