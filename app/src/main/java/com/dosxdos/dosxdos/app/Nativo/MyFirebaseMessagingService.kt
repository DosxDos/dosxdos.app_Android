package com.dosxdos.dosxdos.app.Nativo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dosxdos.dosxdos.app.MainActivity
import com.dosxdos.dosxdos.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            // Mensaje de datos recibido
            Log.d("Firebase", "Mensaje de datos recibido: ${remoteMessage.data}")

            // Extraer el mensaje de los datos de Firebase
            val messageData = remoteMessage.data

            // Verifica si el mensaje no está vacío
            if (messageData != null && messageData.isNotEmpty()) {
                // Convertir el Map a JSON
                val jsonMessage = Gson().toJson(messageData)

                Log.d("Firebase", "Mensaje de datos recibido: ${jsonMessage}")

                // Enviar un Broadcast para notificar a MainActivity con el mensaje
                val intent = Intent("com.dosxdos.dosxdos.FIREBASE_MESSAGE") // Asegúrate de que la acción sea correcta
                intent.putExtra("firebaseMessage", jsonMessage)
                sendBroadcast(intent)  // Enviar el mensaje a través del BroadcastReceiver

                // Mostrar la notificación (opcional)
                showNotification(remoteMessage)
            }
        }
    }



    private fun showNotification(message: RemoteMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crea el canal de notificación para Android 8.0 y versiones superiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default_channel"
            val channelName = "Default Notifications"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }


        // Crear un Intent para abrir la MainActivity y pasarle la URL
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("url", message.data["url"])  // Pasamos la URL a la actividad
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        Log.d(TAG, "Mensaje de datos recibido: ${message.data}")
        // Prepara la notificación
        val notificationBuilder = NotificationCompat.Builder(this, "default_channel")
            .setContentTitle(message.data["title"] ?: "Notificación")
            .setContentText(message.data["body"] ?: "Contenido de la notificación")
            .setSmallIcon(R.drawable.isotipo_con_pastilla_35)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Muestra la notificación
        notificationManager.notify(0, notificationBuilder.build())
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token recibido: $token")
        // Aquí puedes almacenar el token o enviarlo a tu servidor si es necesario
        val notificaciones = Notificaciones(applicationContext)
        notificaciones.saveFCMToken(token)  // Almacena el token usando tu clase
    }

}
