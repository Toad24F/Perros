package com.example.huellasseguras.Notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.huellasseguras.R

object NotificationHelper {

    private const val CHANNEL_ID   = "geofence_alerts"
    private const val CHANNEL_NAME = "Alertas de zona segura"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones cuando tu mascota sale de la zona segura"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun sendGeofenceAlert(context: Context, petName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pet)
            .setContentTitle("⚠️ $petName salió de la zona segura")
            .setContentText("$petName está fuera de la geovalla. Revisa su ubicación.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // ID único por mascota para no acumular notificaciones
        val notifId = petName.hashCode()
        manager.notify(notifId, notification)
    }

    fun cancelGeofenceAlert(context: Context, petName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(petName.hashCode())
    }
}