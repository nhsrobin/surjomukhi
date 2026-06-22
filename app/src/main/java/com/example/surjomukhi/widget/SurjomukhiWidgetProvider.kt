package com.example.surjomukhi.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.R
import com.example.surjomukhi.StatusType
import com.example.surjomukhi.SyncEngine

class SurjomukhiWidgetProvider : AppWidgetProvider() {
    private val TAG = "SurjomukhiWidget"

    companion object {
        const val ACTION_PING = "com.example.surjomukhi.ACTION_PING"
        const val ACTION_SET_FREE = "com.example.surjomukhi.ACTION_SET_FREE"
        const val ACTION_SET_STUDY = "com.example.surjomukhi.ACTION_SET_STUDY"
        const val ACTION_SET_SLEEP = "com.example.surjomukhi.ACTION_SET_SLEEP"
        const val ACTION_SET_BUSY = "com.example.surjomukhi.ACTION_SET_BUSY"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "Widget Intent Received: ${intent.action}")

        val syncEngine = SyncEngine(context)

        when (intent.action) {
            ACTION_PING -> {
                Log.d(TAG, "Triggering ping from widget...")
                syncEngine.sendVibrationPing()
            }
            ACTION_SET_FREE -> {
                Log.d(TAG, "Set state FREE from widget...")
                syncEngine.updateMyStatus(StatusType.FREE.key, "")
            }
            ACTION_SET_STUDY -> {
                Log.d(TAG, "Set state STUDYING from widget...")
                syncEngine.updateMyStatus(StatusType.STUDYING.key, "")
            }
            ACTION_SET_SLEEP -> {
                Log.d(TAG, "Set state SLEEPING from widget...")
                syncEngine.updateMyStatus(StatusType.SLEEPING.key, "")
            }
            ACTION_SET_BUSY -> {
                Log.d(TAG, "Set state BUSY from widget...")
                syncEngine.updateMyStatus(StatusType.BUSY.key, "")
            }
        }

        // Clean up syncEngine thread pool
        syncEngine.shutdown()

        // Force refresh all instantiated widgets in system
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, SurjomukhiWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        for (appWidgetId in appWidgetIds) {
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidgetContent(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.surjomukhi_widget_layout)

        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val partnerNick = prefs.getString("partner_nickname", "Partner") ?: "Partner"
        val otherStatusKey = prefs.getString("partner_status", "sleeping") ?: "sleeping"
        val otherCustom = prefs.getString("partner_custom", "") ?: ""

        val otherStatus = StatusType.fromKey(otherStatusKey)

        // Setup texts & indicators without technical jargon
        val otherLabelText = if (otherStatus == StatusType.CUSTOM && otherCustom.isNotEmpty()) {
            "\"$otherCustom\" 🔮"
        } else {
            "${otherStatus.labelEn} " + when (otherStatus) {
                StatusType.FREE -> "🟢"
                StatusType.STUDYING -> "🟡"
                StatusType.SLEEPING -> "🔵"
                StatusType.SCROLLING -> "📱"
                StatusType.TALKING -> "📞"
                StatusType.HOME -> "🏠"
                StatusType.OFFLINE -> "📡"
                StatusType.BUSY -> "🔴"
                StatusType.CUSTOM -> "🔮"
            }
        }

        views.setTextViewText(R.id.widget_other_status_text, otherLabelText)
        
        // Emotional & Character-driven descriptions (Pure Bengali/English cozy translations)
        val descriptionText = when (otherStatus) {
            StatusType.FREE -> "$partnerNick এখন একদম ফ্রি ও আনন্দের মেজাজে আছে! 😎"
            StatusType.STUDYING -> "$partnerNick গভীর মনোযোগ সহকারে জ্ঞানার্জন কোরছে! 🧠"
            StatusType.SLEEPING -> "$partnerNick ঘুমের রাজ্যে শান্তির সাগরে হারিয়ে গেছে! 💤"
            StatusType.SCROLLING -> "$partnerNick আপন মনে স্ক্রোল করে সময় কাটাচ্ছে! 📱"
            StatusType.TALKING -> "$partnerNick কারোর সাথে মিষ্টি আলাপে মেতে উঠেছে! 📞"
            StatusType.HOME -> "$partnerNick ঘরে ফিরে এখন সুরক্ষিত ও শান্ত আছে! 🏠"
            StatusType.OFFLINE -> "$partnerNick এখন প্রকৃতির কাছাকাছি বা নেটওয়ার্কের বাইরে! 📡"
            StatusType.BUSY -> "$partnerNick খুব জরুরি কাজে ব্যস্ত আছে, একটু পর ডাকুন! 🔥"
            StatusType.CUSTOM -> "$partnerNick বিশেষ একটা মেজাজে সময় কাটাচ্ছে! 🔮"
        }
        views.setTextViewText(R.id.widget_other_desc, descriptionText)

        // Dynamic 3D avatar emoji
        val avatarVibe = when (otherStatus) {
            StatusType.FREE -> "😎"
            StatusType.STUDYING -> "🧠"
            StatusType.SLEEPING -> "😴"
            StatusType.SCROLLING -> "📱"
            StatusType.TALKING -> "📞"
            StatusType.HOME -> "🏠"
            StatusType.OFFLINE -> "📡"
            StatusType.BUSY -> "🔥"
            StatusType.CUSTOM -> "🔮"
        }
        views.setTextViewText(R.id.widget_partner_avatar, avatarVibe)
        
        // Cozy, non-jargon color highlights
        val neonColorInt = when (otherStatus) {
            StatusType.FREE -> 0xFF00FF88.toInt()
            StatusType.STUDYING -> 0xFFFFD700.toInt()
            StatusType.SLEEPING -> 0xFF3B82F6.toInt()
            StatusType.SCROLLING -> 0xFFEC4899.toInt()
            StatusType.TALKING -> 0xFF14B8A6.toInt()
            StatusType.HOME -> 0xFFF59E0B.toInt()
            StatusType.OFFLINE -> 0xFF9CA3AF.toInt()
            StatusType.BUSY -> 0xFFEF4444.toInt()
            StatusType.CUSTOM -> 0xFFA855F7.toInt()
        }
        views.setTextColor(R.id.widget_other_status_text, neonColorInt)

        // Register Pending intents for control buttons
        views.setOnClickPendingIntent(R.id.widget_ping_btn, getPendingSelfIntent(context, ACTION_PING))
        views.setOnClickPendingIntent(R.id.btn_status_free, getPendingSelfIntent(context, ACTION_SET_FREE))
        views.setOnClickPendingIntent(R.id.btn_status_focus, getPendingSelfIntent(context, ACTION_SET_STUDY))
        views.setOnClickPendingIntent(R.id.btn_status_sleep, getPendingSelfIntent(context, ACTION_SET_SLEEP))
        views.setOnClickPendingIntent(R.id.btn_status_busy, getPendingSelfIntent(context, ACTION_SET_BUSY))

        // Instruct AppWidgetManager to refresh
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SurjomukhiWidgetProvider::class.java).apply {
            this.action = action
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
    }
}
