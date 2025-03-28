package com.dosxdos.dosxdos.app.Nativo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Notificaciones(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    private val tokenKey = "fcm_token"

    init {
        // Llama a la función suspendida dentro de una coroutine en el init
        CoroutineScope(Dispatchers.Main).launch {
            getFCMTokenIfNeeded()
        }
    }

    private suspend fun getFCMTokenIfNeeded() {
        // Verifica si ya está almacenado el token en SharedPreferences.
        val storedToken = sharedPreferences.getString(tokenKey, null)
        if (storedToken == null) {
            try {
                // Si no está almacenado, solicita el token de FCM.
                val token = FirebaseMessaging.getInstance().token.await() // Usa await para esperar el token
                // Almacena el token en SharedPreferences si la obtención es exitosa.
                saveFCMToken(token)
            } catch (e: Exception) {
                // Maneja el error si no se obtiene el token.
                println("Error al obtener el token de FCM: ${e.message}")
            }
        }
    }

    fun saveFCMToken(token: String) {
        // Almacena el token en SharedPreferences.
        val editor = sharedPreferences.edit()
        editor.putString(tokenKey, token)
        editor.apply()
    }

    // Método actualizado para obtener el token con un callback
    fun getStoredToken(callback: (String?) -> Unit) {
        //Esta coroutina se asegura de que se ejecute en el hilo principal de arriba a abajo
        CoroutineScope(Dispatchers.Main).launch {
            getFCMTokenIfNeeded()
            val token = sharedPreferences.getString(tokenKey, null)
            Log.d("Notificaciones", "Token recuperado: $token")
            callback(token)  // Llama al callback con el valor recuperado
        }
    }
}
