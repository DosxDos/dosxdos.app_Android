package com.dosxdos.dosxdos.app.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dosxdos.dosxdos.app.R
import com.dosxdos.dosxdos.app.Splash.Splash
import com.dosxdos.dosxdos.app.databinding.ActivityErrorPermisosBinding

class ErrorPermisos : AppCompatActivity() {

    private lateinit var binding: ActivityErrorPermisosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorPermisosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el botón para solicitar permisos
        binding.requestPermissionsButton.setOnClickListener {
            // Solicitar permisos nuevamente cuando el usuario presiona el botón
            checkAndRequestPermissions()
        }

    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = getRequiredPermissions()

        // Filtrar los permisos que no están otorgados
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Si no se necesitan permisos, navegar a SplashActivity
        if (permissionsNeeded.isEmpty()) {
            navigateToSplash()
        } else {
            // Si faltan permisos, verificar si debemos pedirlos nuevamente
            val shouldRequestPermissions = permissionsNeeded.all { permission ->
                shouldShowRationale(permission) // Si el usuario rechazó sin "no volver a preguntar", lo mostramos de nuevo
            }

            if (shouldRequestPermissions) {
                // Solicitar los permisos
                requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
            } else {
                // Si el usuario seleccionó "no volver a preguntar", mostramos mensaje para habilitar manualmente en configuración
                Toast.makeText(
                    this,
                    "Por favor, habilita los permisos desde la configuración del sistema.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Verificar si todos los permisos fueron otorgados
            val allPermissionsGranted = permissions.values.all { it }

            if (allPermissionsGranted) {
                // Si todos los permisos fueron otorgados, navegar a SplashActivity
                navigateToSplash()
            } else {
                // Si no todos los permisos fueron otorgados, mostrar un mensaje
                Toast.makeText(
                    this,
                    "No todos los permisos fueron otorgados. La aplicación no puede continuar.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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

    private fun navigateToSplash() {
        // Navegar a la actividad Splash si todos los permisos están otorgados
        val intent = Intent(this, Splash::class.java)
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }

    private fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }
}
