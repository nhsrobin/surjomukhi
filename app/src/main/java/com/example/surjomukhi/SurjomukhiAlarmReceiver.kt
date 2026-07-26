package com.example.surjomukhi

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.surjomukhi.widget.SurjomukhiWidgetProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SurjomukhiAlarmReceiver : BroadcastReceiver() {
    private val TAG = "SurjomukhiAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "Alarm action received: $action")

        // 1. Schedule the next alarm first to keep the background sync loop alive
        scheduleNextAlarm(context)

        // 2. Perform background sync in a Coroutine using goAsync()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                performSync(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform silent background sync", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun performSync(context: Context) {
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val isBgSyncEnabled = prefs.getBoolean("is_background_sync_enabled", true)
        
        if (!isBgSyncEnabled) {
            Log.d(TAG, "Silent background sync is disabled by user.")
            return
        }

        val myPhone = prefs.getString("my_phone", "") ?: ""
        val partnerPhone = prefs.getString("partner_phone", "") ?: ""

        if (myPhone.isEmpty()) {
            Log.d(TAG, "No user is logged in. Skipping sync.")
            return
        }

        Log.d(TAG, "Running silent background sync for $myPhone, partner: $partnerPhone")

        // Initialize Firebase if not done yet
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val optionsBuilder = FirebaseOptions.Builder()
                    .setProjectId("surjojmukhi-app") // আপনার ফায়ারবেস Project ID
                    .setApplicationId("1:592810166563:android:e0325aa3502bb40963e496") // আপনার ফায়ারবেস App ID
                    .setApiKey("AIzaSyCEdgAtPmZGWlEEldgcaiuq7Mx5Yb1OCdY") // আপনার ফায়ারবেস Web API Key
                FirebaseApp.initializeApp(context, optionsBuilder.build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error in AlarmReceiver", e)
            return
        }

        val db = FirebaseFirestore.getInstance()

        // 1. Sync self information (e.g. if partner updated nickname or bindings)
        try {
            val selfDoc = db.collection("users").document(myPhone).get().await()
            if (selfDoc.exists()) {
                val boundPhone = selfDoc.getString("boundPartnerPhone") ?: ""
                val selfNickname = selfDoc.getString("nickname") ?: "আমার মন"
                
                val currentMyNickname = prefs.getString("my_nickname", "আমার মন")
                val currentPartnerPhone = prefs.getString("partner_phone", "")

                val editor = prefs.edit()
                editor.putString("my_nickname", selfNickname)
                
                if (currentPartnerPhone != boundPhone) {
                    editor.putString("partner_phone", boundPhone)
                }
                editor.apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching self doc in background", e)
        }

        // 2. Sync partner information
        if (partnerPhone.isNotEmpty()) {
            try {
                val partnerDoc = db.collection("users").document(partnerPhone).get().await()
                if (partnerDoc.exists()) {
                    val pNickname = partnerDoc.getString("nickname") ?: "কল্পনা"
                    val pStatus = partnerDoc.getString("status") ?: "sleeping"
                    val pCustomText = partnerDoc.getString("customText") ?: ""
                    val pMuteUntil = partnerDoc.getLong("vibeMuteUntil") ?: 0L
                    val pMuteMessage = partnerDoc.getString("vibeMuteMessage") ?: ""
                    val pUpdatedAt = partnerDoc.getLong("updatedAt") ?: System.currentTimeMillis()

                    val cachedStatus = prefs.getString("partner_status", "sleeping") ?: "sleeping"
                    val cachedCustomText = prefs.getString("partner_custom_text", "") ?: ""

                    val isStatusChanged = cachedStatus != pStatus || cachedCustomText != pCustomText

                    val editor = prefs.edit()
                    editor.putString("partner_nickname", pNickname)
                    editor.putString("partner_status", pStatus)
                    editor.putString("partner_custom_text", pCustomText)
                    editor.putLong("partner_mute_until", pMuteUntil)
                    editor.putString("partner_mute_message", pMuteMessage)
                    editor.putLong("partner_last_known_updated_at", pUpdatedAt)
                    editor.apply()

                    // Update widget content immediately
                    triggerWidgetUpdate(context)

                    // If status changed and notifications are enabled, show a status change notification
                    val isNotifEnabled = prefs.getBoolean("is_notification_enabled", true)
                    if (isStatusChanged && isNotifEnabled) {
                        showStatusChangeNotification(context, pNickname, pStatus, pCustomText)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching partner doc in background", e)
            }
        }

        // 3. Check for new vibrations
        try {
            val vibrationsSnapshot = db.collection("vibrations")
                .whereEqualTo("toPhone", myPhone)
                .get()
                .await()

            val lastProcessedVibeTime = prefs.getLong("last_processed_vibration_timestamp", 0L)
            var maxVibeTime = lastProcessedVibeTime
            var newVibeDetected = false
            var senderPhone = ""

            for (doc in vibrationsSnapshot.documents) {
                val timestamp = doc.getLong("timestamp") ?: 0L
                if (timestamp > lastProcessedVibeTime) {
                    if (timestamp > maxVibeTime) {
                        maxVibeTime = timestamp
                    }
                    newVibeDetected = true
                    senderPhone = doc.getString("fromPhone") ?: ""
                }
            }

            if (newVibeDetected) {
                Log.d(TAG, "New vibration detected at $maxVibeTime from $senderPhone!")
                prefs.edit().putLong("last_processed_vibration_timestamp", maxVibeTime).apply()

                // Check if muted
                val myMuteUntil = prefs.getLong("my_mute_until", 0L)
                val isMuted = myMuteUntil > System.currentTimeMillis()

                if (!isMuted) {
                    // Trigger physical vibration
                    val isVibrationEnabled = prefs.getBoolean("is_vibration_enabled", true)
                    if (isVibrationEnabled) {
                        val durationMs = prefs.getLong("vibration_duration_ms", 450L)
                        triggerPhysicalVibration(context, durationMs)
                    }

                    // Show notification
                    val isNotifEnabled = prefs.getBoolean("is_notification_enabled", true)
                    if (isNotifEnabled) {
                        val partnerNickname = prefs.getString("partner_nickname", "কল্পনা") ?: "কল্পনা"
                        showVibrationNotification(context, partnerNickname)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking vibrations in background", e)
        }
    }

    private fun triggerPhysicalVibration(context: Context, durationMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate device", e)
        }
    }

    private fun showVibrationNotification(context: Context, senderName: String) {
        val channelId = "surjomukhi_vibe_channel"
        val notificationId = 2001
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Surjomukhi Vibrations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming heart connection vibrations"
                vibrationPattern = longArrayOf(0, 450, 150, 450)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val isBn = prefs.getBoolean("is_bangla_lang", true)

        val title = if (isBn) "সঙ্গীর স্পর্শ! 🌻" else "Partner's Touch! 🌻"
        val content = if (isBn) "\"$senderName\" আপনাকে আলতো স্পর্শ পাঠিয়েছেন!" else "\"$senderName\" sent you a gentle vibe!"

        // Add action button to mute
        val muteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.surjomukhi.ACTION_MUTE"
            putExtra("notificationId", notificationId)
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val muteText = if (isBn) "১ ঘণ্টার জন্য মিউট" else "Mute for 1 Hr"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode, muteText, mutePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showStatusChangeNotification(context: Context, partnerName: String, statusKey: String, customText: String) {
        val channelId = "surjomukhi_status_channel"
        val notificationId = 2002
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Surjomukhi Status Changes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when your partner updates their presence status"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val status = StatusType.fromKey(statusKey)
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val isBn = prefs.getBoolean("is_bangla_lang", true)

        val statusLabel = if (status == StatusType.CUSTOM) {
            if (customText.isNotEmpty()) "\"$customText\" 🔮" else (if (isBn) "বিশেষ মেজাজ 🔮" else "Special Mood 🔮")
        } else {
            when (status) {
                StatusType.FREE -> if (isBn) "ফ্রি 🟢" else "Free 🟢"
                StatusType.STUDYING -> if (isBn) "পড়াশোনা করছেন 🧠" else "Studying 🧠"
                StatusType.SLEEPING -> if (isBn) "ঘুমাচ্ছেন 💤" else "Sleeping 💤"
                StatusType.SCROLLING -> if (isBn) "স্ক্রলিং করছেন 📱" else "Scrolling 📱"
                StatusType.TALKING -> if (isBn) "কথা বলছেন 📞" else "Talking 📞"
                StatusType.HOME -> if (isBn) "বাসায় আছেন 🏠" else "At Home 🏠"
                StatusType.OFFLINE -> if (isBn) "নেটের বাইরে 📡" else "Offline 📡"
                StatusType.BUSY -> if (isBn) "ব্যস্ত আছেন 🔴" else "Busy 🔴"
                StatusType.CUSTOM -> if (isBn) "বিশেষ মেজাজ 🔮" else "Special Mood 🔮"
            }
        }

        val title = if (isBn) "সঙ্গীর মনের খবর পরিবর্তন" else "Partner Status Updated"
        val content = if (isBn) "\"$partnerName\" এর বর্তমান অবস্থা: $statusLabel" else "\"$partnerName\" is now: $statusLabel"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SurjomukhiWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
            val provider = SurjomukhiWidgetProvider()
            ids.forEach { id ->
                provider.updateWidgetContent(context, appWidgetManager, id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh widget from AlarmReceiver", e)
        }
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            val isBgSyncEnabled = prefs.getBoolean("is_background_sync_enabled", true)
            if (!isBgSyncEnabled) {
                Log.d("SurjomukhiAlarm", "Sync disabled, not scheduling next alarm.")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SurjomukhiAlarmReceiver::class.java).apply {
                action = "com.example.surjomukhi.ACTION_SYNC"
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            // Trigger every 45 seconds to keep synced and robust without battery drainage
            val triggerAt = System.currentTimeMillis() + 45_000

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
                Log.d("SurjomukhiAlarm", "Successfully scheduled next silent sync alarm in 45 seconds")
            } catch (e: Exception) {
                Log.e("SurjomukhiAlarm", "Failed to schedule exact alarm, falling back to inexact", e)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    }
                } catch (ex: Exception) {
                    Log.e("SurjomukhiAlarm", "Fallback schedule failed too", ex)
                }
            }
        }

        fun stopAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SurjomukhiAlarmReceiver::class.java).apply {
                action = "com.example.surjomukhi.ACTION_SYNC"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            alarmManager.cancel(pendingIntent)
            Log.d("SurjomukhiAlarm", "Sync alarm cancelled successfully")
        }
    }
}
