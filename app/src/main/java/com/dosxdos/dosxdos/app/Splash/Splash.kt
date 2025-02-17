package com.dosxdos.dosxdos.app.Splash

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dosxdos.dosxdos.app.MainActivity
import com.dosxdos.dosxdos.app.R
import com.dosxdos.dosxdos.app.databinding.ActivitySplashBinding
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInput
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors


class Splash : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding // Binding para la UI
    private val urlsToCache = listOf(
        "https://dosxdos.app.iidos.com/index.html?utm_source=web_app_manifest",
        "https://dosxdos.app.iidos.com/index.html",
        "https://dosxdos.app.iidos.com/manifest.json",
        "https://dosxdos.app.iidos.com/sw.js",
        "https://dosxdos.app.iidos.com/firebase-messaging-sw.js",
        "https://dosxdos.app.iidos.com/serviceworker.js",
        "https://dosxdos.app.iidos.com/rutas_montador.html",
        "https://dosxdos.app.iidos.com/ruta_montador.html",
        "https://dosxdos.app.iidos.com/linea_montador.html",
        "https://dosxdos.app.iidos.com/fotos_y_firmas.html",
        "https://dosxdos.app.iidos.com/ot_completa.html",
        "https://dosxdos.app.iidos.com/ot.html",
        "https://dosxdos.app.iidos.com/css/fuentes/Roboto/Roboto-Light.ttf",
        "https://dosxdos.app.iidos.com/css/fuentes/Merriweather/Merriweather-Light.ttf",
        "https://dosxdos.app.iidos.com/css/fuentes/Merriweather/Merriweather-Bold.ttf",
        "https://dosxdos.app.iidos.com/css/fuentes/Lora/Lora-Regular.ttf",
        "https://dosxdos.app.iidos.com/css/fuentes/Lora/Lora-Medium.ttf",
        "https://dosxdos.app.iidos.com/css/fuentes/Lora/Lora-Bold.ttf",
        "https://dosxdos.app.iidos.com/img/candado.png",
        "https://dosxdos.app.iidos.com/img/casa.png",
        "https://dosxdos.app.iidos.com/img/casaWhite.png",
        "https://dosxdos.app.iidos.com/img/dosxdos.png",
        "https://dosxdos.app.iidos.com/img/email.png",
        "https://dosxdos.app.iidos.com/img/flechaAbajo.png",
        "https://dosxdos.app.iidos.com/img/fondo.jpg",
        "https://dosxdos.app.iidos.com/img/logoPwa1024.png",
        "https://dosxdos.app.iidos.com/img/logoPwa512.png",
        "https://dosxdos.app.iidos.com/img/logoPwa384.png",
        "https://dosxdos.app.iidos.com/img/logoPwa256.png",
        "https://dosxdos.app.iidos.com/img/logoPwa192.png",
        "https://dosxdos.app.iidos.com/img/logoPwa128.png",
        "https://dosxdos.app.iidos.com/img/logoPwa96.png",
        "https://dosxdos.app.iidos.com/img/logoPwa64.png",
        "https://dosxdos.app.iidos.com/img/logoPwa32.png",
        "https://dosxdos.app.iidos.com/img/logoPwa16.png",
        "https://dosxdos.app.iidos.com/img/logo300.png",
        "https://dosxdos.app.iidos.com/img/lupa.png",
        "https://dosxdos.app.iidos.com/img/reloj.png",
        "https://dosxdos.app.iidos.com/img/relojWhite.png",
        "https://dosxdos.app.iidos.com/img/usuario.png",
        "https://dosxdos.app.iidos.com/img/rutasWhite.png",
        "https://dosxdos.app.iidos.com/img/cerrar.png",
        "https://dosxdos.app.iidos.com/img/usuarios.png",
        "https://dosxdos.app.iidos.com/img/trash.png",
        "https://dosxdos.app.iidos.com/img/folder.png",
        "https://dosxdos.app.iidos.com/img/comprimido.png",
        "https://dosxdos.app.iidos.com/img/task.png",
        "https://dosxdos.app.iidos.com/img/work.png",
        "https://dosxdos.app.iidos.com/css/cdn_data_tables.css",
        "https://dosxdos.app.iidos.com/js/jquery.js",
        "https://dosxdos.app.iidos.com/js/data_tables.js",
        "https://dosxdos.app.iidos.com/js/cdn_data_tables.js",
        "https://dosxdos.app.iidos.com/js/index_db.js",
        "https://dosxdos.app.iidos.com/img/tienda.png",
        "https://dosxdos.app.iidos.com/img/clientes.png",
        "https://dosxdos.app.iidos.com/img/editar.png",
        "https://dosxdos.app.iidos.com/img/archivar.png",
        "https://dosxdos.app.iidos.com/img/back.png",
        "https://dosxdos.app.iidos.com/img/visible.png",
        "https://dosxdos.app.iidos.com/img/no_visible.png",
        "https://dosxdos.app.iidos.com/img/crear.png",
        "https://dosxdos.app.iidos.com/img/logo_clientes.png",
        "https://dosxdos.app.iidos.com/js/index_db.js",
        "https://dosxdos.app.iidos.com/img/logo2930.png",
        "https://dosxdos.app.iidos.com/img/instalar.png",
        "https://dosxdos.app.iidos.com/espanol.json",
        "https://dosxdos.app.iidos.com/english.json",
        "https://dosxdos.app.iidos.com/pv.html",
        "https://dosxdos.app.iidos.com/crear_pv.html",
        "https://dosxdos.app.iidos.com/editar_pv.html",
        "https://dosxdos.app.iidos.com/editar_pv.html",
        "https://dosxdos.app.iidos.com/js/fixed_header.js",
        "https://dosxdos.app.iidos.com/css/fuentes/Futura/Futura_Bold.otf",
        "https://dosxdos.app.iidos.com/css/fuentes/Futura/Futura_Light.otf",
        "https://dosxdos.app.iidos.com/css/fuentes/Futura/Futura_Medium.otf",
        "https://dosxdos.app.iidos.com/img/alerta.png",
        "https://dosxdos.app.iidos.com/img/saludo.png",
        "https://dosxdos.app.iidos.com/img/dm.png",
        "https://dosxdos.app.iidos.com/img/papelera.png",
        "https://dosxdos.app.iidos.com/horarios.html",
        "https://dosxdos.app.iidos.com/lineas_ot.html",
        "https://dosxdos.app.iidos.com/lineas.html",
        "https://dosxdos.app.iidos.com/usuarios_oficina.html",
        "https://dosxdos.app.iidos.com/dm.html",
        "https://dosxdos.app.iidos.com/reciclar.html",
        "https://dosxdos.app.iidos.com/rutas_inactivas.html",
        "https://dosxdos.app.iidos.com/historial_montador.html",
        "https://dosxdos.app.iidos.com/linea_historial_montador.html",
        "https://dosxdos.app.iidos.com/img/trabajos.png",
        "https://dosxdos.app.iidos.com/img/clientes2.png",
        "https://dosxdos.app.iidos.com/img/sincronizar.png",
        "https://dosxdos.app.iidos.com/img/historial.png",
        "https://dosxdos.app.iidos.com/css/tailwindmain.css"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicia la carga de caché y luego pasa a la siguiente actividad
        cacheResources()
    }

    private fun cacheResources() {
        val executor = Executors.newFixedThreadPool(4) // Descarga en paralelo

        if (isNetworkAvailable()) {
            Log.d("Splash", "Internet disponible. Descargando archivos...")
            urlsToCache.forEach { url ->
                executor.execute { saveToCache(url) }
            }
        } else {
            Log.d("Splash", "Sin conexión. Usando archivos en caché si están disponibles.")
        }

        executor.shutdown()
        while (!executor.isTerminated) {
            Thread.sleep(200) // Espera que termine la caché
        }
        runOnUiThread { inicializarVistaPrincipal() }
    }

    private fun saveToCache(urlString: String) {
        try {
            val uri = Uri.parse(urlString)
            val fileName = uri.lastPathSegment ?: return
            val cacheFile = File(filesDir, fileName) // 📌 Usa filesDir en lugar de cacheDir

            // Si el archivo ya existe, lo borro para volver a cachearlo

            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.d("Splash", "Archivo en caché reemplazado: $fileName")
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(cacheFile)

                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                Log.d("Splash", "Archivo guardado en caché: $fileName")
            } else {
                Log.e("Splash", "Error al descargar archivo: ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("Splash", "Error al descargar archivo: $urlString", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun inicializarVistaPrincipal() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Cierra el splash después de iniciar MainActivity
    }
}
