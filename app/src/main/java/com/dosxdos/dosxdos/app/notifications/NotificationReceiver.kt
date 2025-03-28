package com.dosxdos.dosxdos.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver(
    private val onMessageReceived: (String?) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val jsonMessage = intent?.getStringExtra("firebaseMessage")
        Log.d("NotificationReceiver", "🔔 Mensaje recibido: $jsonMessage")
        onMessageReceived(jsonMessage)
    }
}
