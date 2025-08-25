package com.example.nualia3.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.nualia3.R
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Receptor que se ejecuta cuando suena una alarma programada.
 * Muestra una notificación local con el título y mensaje de la entrada correspondiente,
 * y luego desactiva el campo `notificar` en Firestore para esa entrada.
 */
class ReceptorNotificacion : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Sacamos datos del intent (si alguno viene nulo, usamos un texto por defecto)
        val titulo = intent.getStringExtra("titulo") ?: "Recordatorio"
        val mensaje = intent.getStringExtra("mensaje") ?: "Tienes un evento pendiente"
        val entradaId = intent.getStringExtra("entradaId")
        val usuarioId = intent.getStringExtra("usuarioId")

        val canalId = "recordatorio_eventos"

        // Creamos el canal de notificación solo si estamos en Android 8 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Recordatorios de eventos",
                NotificationManager.IMPORTANCE_HIGH
            )
            // Sacamos el NotificationManager y registramos el canal si es necesario
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }

        // Preparamos la notificación con título, mensaje e icono
        val builder = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(R.drawable.ic_nualia_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Mostramos la notificación con un ID único basado en el tiempo actual
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())

        // Ponemos el campo notificar = false en la entrada de Firestore (si tenemos IDs)
        if (!entradaId.isNullOrEmpty() && !usuarioId.isNullOrEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(entradaId)
                .update("notificar", false)
        }
    }
}
