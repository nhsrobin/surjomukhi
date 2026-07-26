package com.example.surjomukhi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class NotificationActionReceiver : BroadcastReceiver() {
    private val TAG = "NotificationActionRec"

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val notificationId = intent.getIntExtra("notificationId", 1001)

        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        if (action == "com.example.surjomukhi.ACTION_MUTE") {
            Log.d(TAG, "Mute action triggered from notification")
            
            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            val myPhone = prefs.getString("my_phone", "") ?: ""
            
            if (myPhone.isNotEmpty()) {
                // Set mute for 60 minutes (1 hour)
                val muteDurationMs = 60 * 60 * 1000L
                val muteUntil = System.currentTimeMillis() + muteDurationMs
                val muteMsg = "Muted from Notification"

                // Save locally
                prefs.edit()
                    .putLong("my_mute_until", muteUntil)
                    .putString("my_mute_message", muteMsg)
                    .apply()

                // Sync with Firestore
                try {
                    val firebaseApp = if (FirebaseApp.getApps(context).isEmpty()) {
                        FirebaseApp.initializeApp(context)
                    } else {
                        FirebaseApp.getInstance()
                    }
                    if (firebaseApp != null) {
                        val db = FirebaseFirestore.getInstance(firebaseApp)
                        db.collection("users").document(myPhone).update(
                            mapOf(
                                "vibeMuteUntil" to muteUntil,
                                "vibeMuteMessage" to muteMsg
                            )
                        ).addOnSuccessListener {
                            Log.d(TAG, "Mute synced with database successfully from BroadcastReceiver")
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Failed to sync mute with database from BroadcastReceiver", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase initialization error in receiver", e)
                }
            }
        } else if (action == "com.example.surjomukhi.ACTION_SILENT") {
            Log.d(TAG, "Silent action triggered from notification - dismissed")
        }
    }
}
