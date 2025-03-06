package com.dosxdos.dosxdos.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dosxdos.dosxdos.app.Activities.ErrorPermisos
import com.dosxdos.dosxdos.app.Clases.FileData
import com.dosxdos.dosxdos.app.Nativo.Notificaciones
import com.dosxdos.dosxdos.app.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Clase de binding generada automáticamente
    private val FILE_CHOOSER_REQUEST_CODE = 1002
    private lateinit var firebaseTokenManager: Notificaciones
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    // Crear el BroadcastReceiver para recibir mensajes de Firebase
    private val firebaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Obtener el mensaje de los datos extraídos del Intent
            val jsonMessage = intent?.getStringExtra("firebaseMessage")
            Log.d("WebView", "Mensaje recibido en MainActivity: $jsonMessage")
            injectMessageIntoWebView(jsonMessage)
        }
    }

    // Definir los permisos requeridos de acuerdo a la versión de Android
    private val REQUIRED_PERMISSIONS = getRequiredPermissions()

    // Función que devuelve los permisos requeridos según la versión de Android
    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) y superior
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) - Scoped Storage, pero con permisos para leer/escribir archivos específicos
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_EXTERNAL_STORAGE, // Permiso para leer archivos específicos
                Manifest.permission.WRITE_EXTERNAL_STORAGE // Permiso para escribir archivos específicos
            )
        } else {
            // Para versiones inferiores a Android 10, permisos tradicionales de almacenamiento
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }


    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filterValues { !it }.keys
            if (deniedPermissions.isNotEmpty()) {

                Toast.makeText(this, "Permisos denegados: $deniedPermissions", Toast.LENGTH_LONG).show()
                val intent = Intent(this, ErrorPermisos::class.java)
                startActivity(intent)
                finish() // Finaliza la actividad actual
            } else {
                // Ahora, carga el WebView y realiza otras configuraciones
                logica() // 🔹 Solo ahora que los permisos fueron concedidos

            }
        }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Establece el layout con el binding

        // Registrar el receiver solo si el SDK es Oreo (API 26) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter("com.dosxdos.dosxdos.FIREBASE_MESSAGE")
            registerReceiver(firebaseReceiver, filter, Context.RECEIVER_EXPORTED)
        }



        requestPermissions() // 🔹 Solicitar permisos al iniciar la app
        val url = intent.getStringExtra("url")
        // Verificar si los permisos ya están concedidos y ejecutar la lógica
        if (arePermissionsGranted() && url == null) {
            logica() // Ejecuta la lógica si los permisos ya están concedidos
        }
        // Verifica si hay una URL pasada a través del Intent
        if (url != null) {
            // Cargar la URL en el WebView
            logica(url)
        }
        // Configura SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Recargar el WebView
            binding.webView.reload()

            // Detener la animación de refresco después de recargar
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    // Método para inyectar el mensaje en el WebView usando JavaScript
    private fun injectMessageIntoWebView(message: String?) {
        val webView = binding.webView
        val script = """
            javascript:(function() {
                window.localStorage.setItem('dataNotificacionNativa', '$message');
                console.log('Mensaje guardado en localStorage: ' + '$message');
                notificarWebApp()
            })()
        """
        webView.evaluateJavascript(script) { result ->
            Log.d("WebView", "Resultado del script: $result")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el BroadcastReceiver al destruir la actividad
        unregisterReceiver(firebaseReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun arePermissionsGranted() : Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun logica(url: String = "https://dosxdos.app.iidos.com/") {
        val webView = binding.webView
        val webSettings = webView.settings

        webView.addJavascriptInterface(this, "Android")

        // 🔹 Habilitar JavaScript y almacenamiento local
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        webView.settings.domStorageEnabled = true
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

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if(url == "https://dosxdos.app.iidos.com/linea_montador.html" || url == "https://dosxdos.app.iidos.com/mapa_ruta.html") {
                    // Configura SwipeRefreshLayout
                    binding.swipeRefreshLayout.isEnabled = false
                }else{
                    binding.swipeRefreshLayout.isEnabled = true
                }

                // Comprobamos si la URL es del dominio 'dosxdos'
                if (url?.contains("dosxdos.app.iidos.com") == true) {
                    // Si la URL pertenece a dosxdos, la cargamos internamente
                    return super.shouldOverrideUrlLoading(view, url)
                } else {
                    // Si la URL pertenece a un dominio externo, la redirigimos al navegador o la cargamos desde la red
                    if (!isNetworkAvailable()) {
                        Toast.makeText(this@MainActivity, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
                        return true  // No cargamos la URL en el WebView
                    }

                    // Aquí puedes agregar un código para abrir un navegador o continuar la carga de la URL de Google
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Inyectar un script de JavaScript en el WebView que escuche los clics en el campo input[type="file"]
                // Inyectar el script de JavaScript para escuchar el clic en el campo input[type="file"]
                injectFileInputHandler(view)

                injectFilesFunction(view)

                firebaseTokenManager = Notificaciones(this@MainActivity)

                // Obtener el token de manera asincrónica
                firebaseTokenManager.getStoredToken { token ->
                    // Comprobamos si el token no es nulo ni vacío
                    if (!token.isNullOrEmpty()) {
                        if (url == "https://dosxdos.app.iidos.com/index.html") {
                            // Ejecutar el script para almacenar el token en el localStorage
                            val script = """
                        javascript:(function() {
                            window.localStorage.setItem('tokenNativo', '$token');
                            console.log('Token inyectado correctamente en localStorage $token');
                            asignarTokenNativo()                        
                        })()"""
                            // Cargar el script en el WebView
                            view?.loadUrl(script)
                        } else {
                            Log.d("WebView", "El token no se inyecta si no está en la página principal")
                        }
                    } else {
                        Log.d("WebView", "Token vacío, no se inyecta.")
                    }
                }
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()

                // Verificar si la URL pertenece al dominio de dosxdos
                if (url.contains("dosxdos.app.iidos.com")) {
                    // Si es del dominio de dosxdos, proceder con la carga normal
                    return super.shouldInterceptRequest(view, request)
                }

                // Si la URL pertenece a un dominio externo (como Google o anuncios), manejarla según la conexión
                if (!isNetworkAvailable()) {
                    // Si no hay conexión y el archivo está en caché, cargarlo desde la caché
                    val cacheFile = getCachedFile(url)
                    if (cacheFile.exists()) {
                        Log.d("WebView", "Cargando desde caché: ${cacheFile.absolutePath}")
                        return getCachedWebResource(cacheFile, url)
                    } else {
                        // Si no hay archivo en caché, no cargar nada y mostrar un mensaje de error
                        Log.d("WebView", "No hay conexión y no hay archivo en caché para $url")
                        return null  // No cargar nada en el WebView
                    }
                }

                // Si hay conexión a Internet, permitir la solicitud normal
                // Actualizar la caché con el nuevo recurso si es necesario
                val cacheFile = getCachedFile(url)

                overWriteUrl(cacheFile, url)

                return super.shouldInterceptRequest(view, request)
            }


            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

                val url = request?.url.toString()
                val cacheFile = getCachedFile(url)

                if (cacheFile.exists() && !isNetworkAvailable()) {
                    Log.d("WebView", "Cargando desde caché: ${cacheFile.absolutePath}")
                    view?.loadUrl("file://${cacheFile.absolutePath}")
                } else {
                    Log.e("WebView", "Archivo no encontrado en caché. No se puede cargar sin conexión.")
                }
            }
        }
        // Configurar WebChromeClient para manejar la selección de archivos
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                mFilePathCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()
                intent?.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Permitir selección múltiple de archivos

                try {
                    if (intent != null) {
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                    }
                } catch (e: ActivityNotFoundException) {
                    mFilePathCallback?.onReceiveValue(null)
                    mFilePathCallback = null
                    return false
                }
                return true
            }
        }
        // 🔹 Cargar la URL principal
        webView.loadUrl(url)
    }

    private fun injectFileInputHandler(webView: WebView?) {
        webView?.evaluateJavascript("""
        (function() {
            var input = document.querySelector('input[type="file"]');
            if (input) {
                input.addEventListener('click', function(event) {
                    event.preventDefault();  // Evitar la acción predeterminada
                    event.stopPropagation();  // Evitar la propagación del evento
                    
                    // Verificar si ya hemos llamado al método
                    if (!input.hasAttribute('data-clicked')) {
                        input.setAttribute('data-clicked', 'true');  // Marcar el input como "ya clickeado"
                        Android.openFileChooser();  // Llamar a la función de la interfaz nativa
                        console.log('Script inyectado correctamente');

                        // Esperar 1 segundo antes de restablecer el atributo
                        setTimeout(function() {
                            input.removeAttribute('data-clicked');  // Restablecer el atributo después de 1 segundo
                            console.log('data-clicked restablecido después de 1 segundo');
                        }, 1000);
                    }
                });
            } else {
                console.log('No se encontró input[type="file"]');
            }
        })();
    """, { result ->
            Log.d("WebView", "Script inyectado, resultado: $result")
        })
    }




    @JavascriptInterface
    fun openFileChooser() {
        Log.d("WebView", "openFileChooser llamada desde JavaScript")

        // Forzar la apertura del selector de archivos
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"  // O puedes especificar tipos de archivos como "image/*" para solo imágenes
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)  // Permitir la selección de múltiples archivos

        try {
            startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Log.e("WebView", "No hay actividad para manejar la selección de archivos: ${e.message}")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
            // Obtener las URIs de los archivos seleccionados
            val results = if (data != null) {
                getResultUri(data)
            } else {
                null
            }

            // Si los resultados no son nulos, pasarlos al callback
            if (results != null) {
                // Enviar los resultados directamente a la WebView sin inyectar el DOM
                passFilesToWebView(results)
            } else {
                passFilesToWebView(null)
            }
        }
    }

    // Obtener las URIs de los archivos seleccionados
    private fun getResultUri(data: Intent?): Array<Uri>? {
        return if (data?.clipData != null) {
            // Si se seleccionaron múltiples archivos, obtenemos los URIs de clipData
            val itemCount = data.clipData?.itemCount ?: 0
            val uris = Array(itemCount) { i ->
                data.clipData?.getItemAt(i)?.uri ?: Uri.EMPTY
            }
            uris
        } else {
            // Si solo se seleccionó un archivo, obtenemos la URI directamente de data
            arrayOf(data?.data ?: Uri.EMPTY)
        }
    }

    private fun injectFilesFunction(view: WebView?) {
        val jsCode = """
        function injectFilesToInput(files) {
            var input = document.querySelector('input[type="file"]');
            if (input) {
                var dataTransfer = new DataTransfer();

                files.forEach(function(file) {
                    var fileName = file.name;
                    var fileType = file.type;

                    var byteCharacters = atob(file.base64Data); // Decodificar el Base64
                    var byteArrays = [];

                    for (var offset = 0; offset < byteCharacters.length; offset += 512) {
                        var slice = byteCharacters.slice(offset, offset + 512);
                        var byteNumbers = new Array(slice.length);
                        for (var i = 0; i < slice.length; i++) {
                            byteNumbers[i] = slice.charCodeAt(i);
                        }
                        byteArrays.push(new Uint8Array(byteNumbers));
                    }

                    var blob = new Blob(byteArrays, { type: fileType });
                    var newFile = new File([blob], fileName, { type: fileType });

                    dataTransfer.items.add(newFile);
                });

                input.files = dataTransfer.files;

                var event = new Event('change');
                input.dispatchEvent(event);

                console.log('Archivos inyectados correctamente y evento "change" disparado');
            }
        }
    """
        view?.evaluateJavascript(jsCode, null)
    }


    private fun passFilesToWebView(files: Array<Uri>?) {
        val fileObjects = files?.map { fileUri ->
            val base64Data = getFileBase64(fileUri)  // Convierte el archivo a Base64
            val fileName = fileUri.lastPathSegment ?: "unknown"
            FileData(fileName, "image/jpeg", base64Data)  // Asignamos un tipo MIME como ejemplo
        } ?: emptyList()

        // Convertir la lista de objetos a JSON
        val jsonFiles = Gson().toJson(fileObjects)

        // Llamar a la función JavaScript con los archivos seleccionados
        binding.webView.evaluateJavascript("""
        injectFilesToInput($jsonFiles);
    """, null)
    }

    // Función para convertir el archivo a Base64
    private fun getFileBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri) ?: return ""
        val byteArray = inputStream.readBytes()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
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

        // Si hay permisos faltantes, solicitarlos
        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Limpiar el ValueCallback para permitir que se reabra el selector de archivos
        mFilePathCallback?.onReceiveValue(null)
        mFilePathCallback = null

        val webView = binding.webView

        // Si el WebView puede ir atrás en el historial
        if (webView.canGoBack()) {
            // Si el WebView tiene historial, navegar hacia atrás
            webView.goBack()
        } else {
            // Si no hay historial, proceder con el comportamiento predeterminado
            super.onBackPressed()
        }
    }
    var currentUrl: String? = null

    override fun onResume() {
        super.onResume()
        if (currentUrl != null) {
            binding.webView.loadUrl(currentUrl!!)  // Cargar la URL almacenada al reanudar
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentUrl = binding.webView.url // Guarda la URL actual
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Si ya hemos guardado una URL, no se recargará la página por defecto.
        if (currentUrl != null) {
            binding.webView.loadUrl(currentUrl!!)
        }
    }
}