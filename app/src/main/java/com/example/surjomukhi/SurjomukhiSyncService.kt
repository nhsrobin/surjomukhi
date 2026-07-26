package com.example.surjomukhi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class SurjomukhiSyncService : Service() {
    private val TAG = "SurjomukhiSyncService"
    private var syncEngine: SyncEngine? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Background Sync Service Created")
        syncEngine = SyncEngine(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Background Sync Service Started")

        val prefs = getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val isBackgroundSyncEnabled = prefs.getBoolean("is_background_sync_enabled", true)

        if (!isBackgroundSyncEnabled) {
            Log.d(TAG, "Background sync is disabled in settings. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Show foreground notification to run "all time" legally and cleanly on modern Android
        showForegroundNotification()

        // Start Firestore real-time listener
        syncEngine?.startSyncListeners()

        // Make the service sticky so it restarts if killed by the OS
        return START_STICKY
    }

    private fun showForegroundNotification() {
        val channelId = "surjomukhi_sync_channel"
        val notificationId = 1002

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Surjomukhi Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps connection with your partner active in the background"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val prefs = getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val isBn = prefs.getBoolean("is_bangla_lang", true)

        val title = if (isBn) "সূর্যমুখী ব্যাকগ্রাউন্ড সিঙ্ক সক্রিয়" else "Surjomukhi Background Sync Active"
        val content = if (isBn) "আপনার সঙ্গীর মনের খবর রিয়েল-টাইমে সিঙ্ক হচ্ছে... 🌻" else "Syncing partner's presence in real-time... 🌻"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
            // Fallback for safety on older builds or non-manifest mapped scenarios
            try {
                startForeground(notificationId, notification)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback startForeground failed too", ex)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Background Sync Service Destroyed")
        syncEngine?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
