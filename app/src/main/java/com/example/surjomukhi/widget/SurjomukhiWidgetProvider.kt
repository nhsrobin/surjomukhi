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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.surjomukhi.StatusType
import com.example.surjomukhi.SyncEngine
import com.example.surjomukhi.SurjomukhiSyncService

class SurjomukhiWidgetProvider : AppWidgetProvider() {
    private val TAG = "SurjomukhiWidget"

    companion object {
        const val ACTION_PING = "com.example.surjomukhi.ACTION_PING"
        const val ACTION_SET_FREE = "com.example.surjomukhi.ACTION_SET_FREE"
        const val ACTION_SET_STUDY = "com.example.surjomukhi.ACTION_SET_STUDY"
        const val ACTION_SET_SLEEP = "com.example.surjomukhi.ACTION_SET_SLEEP"
        const val ACTION_SET_SCROLL = "com.example.surjomukhi.ACTION_SET_SCROLL"
        const val ACTION_SET_TALK = "com.example.surjomukhi.ACTION_SET_TALK"
        const val ACTION_SET_HOME = "com.example.surjomukhi.ACTION_SET_HOME"
        const val ACTION_SET_OFFLINE = "com.example.surjomukhi.ACTION_SET_OFFLINE"
        const val ACTION_SET_BUSY = "com.example.surjomukhi.ACTION_SET_BUSY"
        const val ACTION_SET_CUSTOM = "com.example.surjomukhi.ACTION_SET_CUSTOM"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Start background sync service to keep Firestore real-time listener alive
        try {
            val serviceIntent = Intent(context, SurjomukhiSyncService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Best effort to start service on widget update: ${e.message}")
        }

        // Keep silent background sync alive
        try {
            com.example.surjomukhi.SurjomukhiAlarmReceiver.scheduleNextAlarm(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule next alarm on widget update", e)
        }

        for (appWidgetId in appWidgetIds) {
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "Widget Intent Received: ${intent.action}")

        // Ensure background sync service is started and active
        try {
            val serviceIntent = Intent(context, SurjomukhiSyncService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Best effort to start service on intent receive: ${e.message}")
        }

        // Keep silent background sync alive
        try {
            com.example.surjomukhi.SurjomukhiAlarmReceiver.scheduleNextAlarm(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule next alarm on intent receive", e)
        }

        val syncEngine = SyncEngine(context)
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)

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
            ACTION_SET_SCROLL -> {
                Log.d(TAG, "Set state SCROLLING from widget...")
                syncEngine.updateMyStatus(StatusType.SCROLLING.key, "")
            }
            ACTION_SET_TALK -> {
                Log.d(TAG, "Set state TALKING from widget...")
                syncEngine.updateMyStatus(StatusType.TALKING.key, "")
            }
            ACTION_SET_HOME -> {
                Log.d(TAG, "Set state HOME from widget...")
                syncEngine.updateMyStatus(StatusType.HOME.key, "")
            }
            ACTION_SET_OFFLINE -> {
                Log.d(TAG, "Set state OFFLINE from widget...")
                syncEngine.updateMyStatus(StatusType.OFFLINE.key, "")
            }
            ACTION_SET_BUSY -> {
                Log.d(TAG, "Set state BUSY from widget...")
                syncEngine.updateMyStatus(StatusType.BUSY.key, "")
            }
            ACTION_SET_CUSTOM -> {
                Log.d(TAG, "Set state CUSTOM from widget...")
                val currentCustomText = prefs.getString("my_custom_text", "") ?: ""
                syncEngine.updateMyStatus(StatusType.CUSTOM.key, currentCustomText.ifEmpty { "বিশেষ মেজাজ" })
            }
        }

        // Clean up syncEngine thread pool
        syncEngine.shutdown()

        // Force refresh all instantiated widgets in system
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context, SurjomukhiWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        for (appWidgetId in appWidgetIds) {
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    fun updateWidgetContent(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.surjomukhi_widget_layout)

        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        val otherStatusKey = prefs.getString("partner_status", "sleeping") ?: "sleeping"
        val otherCustom = prefs.getString("partner_custom_text", "") ?: ""
        val myStatusKey = prefs.getString("my_status", "free") ?: "free"

        val otherStatus = StatusType.fromKey(otherStatusKey)

        // Setup texts & indicators without technical jargon
        val otherLabelText = if (otherStatus == StatusType.CUSTOM) {
            if (otherCustom.isNotEmpty()) "\"$otherCustom\" 🔮" else "विशेष মেজাজ 🔮"
        } else {
            when (otherStatus) {
                StatusType.FREE -> "ফ্রি 🟢"
                StatusType.STUDYING -> "পড়াশোনা 🧠"
                StatusType.SLEEPING -> "ঘুমাচ্ছে 💤"
                StatusType.SCROLLING -> "স্ক্রলিং 📱"
                StatusType.TALKING -> "কথা বলছে 📞"
                StatusType.HOME -> "বাসায় 🏠"
                StatusType.OFFLINE -> "নেটের বাইরে 📡"
                StatusType.BUSY -> "ব্যস্ত 🔴"
                StatusType.CUSTOM -> "विशेष মেজাজ 🔮"
            }
        }

        // Emotional & Character-driven descriptions (Pure Bengali/English cozy translations)
        val descriptionText = when (otherStatus) {
            StatusType.FREE -> "সে এখন একদম ফ্রি ও আনন্দের মেজাজে আছে! 😎"
            StatusType.STUDYING -> "সে গভীর মনোযোগ সহকারে পড়াশোনা করছে! 🧠"
            StatusType.SLEEPING -> "সে ঘুমের রাজ্যে শান্তির সাগরে হারিয়ে গেছে! 💤"
            StatusType.SCROLLING -> "সে আপন মনে স্ক্রোল করে সময় কাটাচ্ছে! 📱"
            StatusType.TALKING -> "সে কারোর সাথে মিষ্টি আলাপে মেতে উঠেছে! 📞"
            StatusType.HOME -> "সে ঘরে ফিরে এখন সুরক্ষিত ও শান্ত আছে! 🏠"
            StatusType.OFFLINE -> "সে এখন প্রকৃতির কাছাকাছি বা নেটওয়ার্কের বাইরে! 📡"
            StatusType.BUSY -> "সে খুব জরুরি কাজে ব্যস্ত আছে, একটু পর ডাকুন! 🔥"
            StatusType.CUSTOM -> if (otherCustom.isNotEmpty()) "সে এখন \"$otherCustom\" অনুভূতি শেয়ার করছে! 🔮" else "সে বিশেষ একটা মেজাজে সময় কাটাচ্ছে! 🔮"
        }

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

        // Compute the target width for text wrapping to ensure pixel-perfect Hind Siliguri text rendering
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val density = context.resources.displayMetrics.density
        val widthDp = if (minWidthDp > 0) minWidthDp else 240
        // The left-column takes roughly 65% of the widget's layout width
        val colWidthPx = (widthDp * 0.65f * density).toInt().coerceAtLeast(300)

        // Render Widget Header
        val headerBitmap = renderTextToBitmap(
            context = context,
            text = "🌻 সূর্যমুখী (Surjomukhi)",
            textSizeSp = 14f,
            textColor = 0xFFFFFFFF.toInt(),
            fontResId = R.font.hind_siliguri_bold,
            maxWidthPx = colWidthPx
        )
        views.setImageViewBitmap(R.id.widget_title, headerBitmap)

        // Render Partner Status Text
        val statusBitmap = renderTextToBitmap(
            context = context,
            text = otherLabelText,
            textSizeSp = 19f,
            textColor = neonColorInt,
            fontResId = R.font.hind_siliguri_bold,
            maxWidthPx = colWidthPx
        )
        views.setImageViewBitmap(R.id.widget_other_status_text, statusBitmap)

        // Render Partner Description Text
        val descBitmap = renderTextToBitmap(
            context = context,
            text = descriptionText,
            textSizeSp = 9.5f,
            textColor = 0xFFA0A0BF.toInt(),
            fontResId = R.font.hind_siliguri_medium,
            maxWidthPx = colWidthPx
        )
        views.setImageViewBitmap(R.id.widget_other_desc, descBitmap)

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

        // Highlight active status button background on widget with glowing 3D colorful strokes
        views.setInt(R.id.btn_status_free, "setBackgroundResource", if (myStatusKey == StatusType.FREE.key) R.drawable.widget_status_free_active else R.drawable.widget_status_free_inactive)
        views.setInt(R.id.btn_status_focus, "setBackgroundResource", if (myStatusKey == StatusType.STUDYING.key) R.drawable.widget_status_focus_active else R.drawable.widget_status_focus_inactive)
        views.setInt(R.id.btn_status_sleep, "setBackgroundResource", if (myStatusKey == StatusType.SLEEPING.key) R.drawable.widget_status_sleep_active else R.drawable.widget_status_sleep_inactive)
        views.setInt(R.id.btn_status_scrolling, "setBackgroundResource", if (myStatusKey == StatusType.SCROLLING.key) R.drawable.widget_status_scrolling_active else R.drawable.widget_status_scrolling_inactive)
        views.setInt(R.id.btn_status_talking, "setBackgroundResource", if (myStatusKey == StatusType.TALKING.key) R.drawable.widget_status_talking_active else R.drawable.widget_status_talking_inactive)
        views.setInt(R.id.btn_status_home, "setBackgroundResource", if (myStatusKey == StatusType.HOME.key) R.drawable.widget_status_home_active else R.drawable.widget_status_home_inactive)
        views.setInt(R.id.btn_status_offline, "setBackgroundResource", if (myStatusKey == StatusType.OFFLINE.key) R.drawable.widget_status_offline_active else R.drawable.widget_status_offline_inactive)
        views.setInt(R.id.btn_status_busy, "setBackgroundResource", if (myStatusKey == StatusType.BUSY.key) R.drawable.widget_status_busy_active else R.drawable.widget_status_busy_inactive)

        // Register Pending intents for control buttons
        views.setOnClickPendingIntent(R.id.widget_heart_ping_btn, getPendingSelfIntent(context, ACTION_PING))
        views.setOnClickPendingIntent(R.id.btn_status_free, getPendingSelfIntent(context, ACTION_SET_FREE))
        views.setOnClickPendingIntent(R.id.btn_status_focus, getPendingSelfIntent(context, ACTION_SET_STUDY))
        views.setOnClickPendingIntent(R.id.btn_status_sleep, getPendingSelfIntent(context, ACTION_SET_SLEEP))
        views.setOnClickPendingIntent(R.id.btn_status_scrolling, getPendingSelfIntent(context, ACTION_SET_SCROLL))
        views.setOnClickPendingIntent(R.id.btn_status_talking, getPendingSelfIntent(context, ACTION_SET_TALK))
        views.setOnClickPendingIntent(R.id.btn_status_home, getPendingSelfIntent(context, ACTION_SET_HOME))
        views.setOnClickPendingIntent(R.id.btn_status_offline, getPendingSelfIntent(context, ACTION_SET_OFFLINE))
        views.setOnClickPendingIntent(R.id.btn_status_busy, getPendingSelfIntent(context, ACTION_SET_BUSY))

        // ------------------ CHIRKUT (চিরকুট) OVERLAY CONTROLLERS ------------------
        val partnerChirkut = prefs.getString("partner_chirkut", "") ?: ""
        val isChirkutEnabledSetting = prefs.getBoolean("is_chirkut_enabled", true)

        if (partnerChirkut.isNotEmpty() && isChirkutEnabledSetting) {
            views.setViewVisibility(R.id.widget_main_container, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_chirkut_container, android.view.View.VISIBLE)

            val chirkutTextBitmap = renderTextToBitmap(
                context = context,
                text = partnerChirkut,
                textSizeSp = 15f,
                textColor = 0xFFFFFFFF.toInt(),
                fontResId = R.font.hind_siliguri_bold,
                maxWidthPx = (colWidthPx * 1.25f).toInt()
            )
            views.setImageViewBitmap(R.id.widget_chirkut_text_image, chirkutTextBitmap)

            val configIntent = Intent(context, com.example.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                111,
                configIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_chirkut_container, configPendingIntent)
        } else {
            views.setViewVisibility(R.id.widget_main_container, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_chirkut_container, android.view.View.GONE)
        }
        // ------------------ END OF CHIRKUT OVERLAY CONTROLLERS --------------------

        // Instruct AppWidgetManager to refresh
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun renderTextToBitmap(
        context: Context,
        text: CharSequence,
        textSizeSp: Float,
        textColor: Int,
        fontResId: Int,
        maxWidthPx: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Bitmap {
        val displayMetrics = context.resources.displayMetrics
        val textSizePx = textSizeSp * displayMetrics.scaledDensity

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSizePx
            this.color = textColor
            try {
                this.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, fontResId)
            } catch (e: Exception) {
                this.typeface = Typeface.DEFAULT
            }
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidthPx)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                textPaint,
                maxWidthPx,
                alignment,
                1.1f,
                0f,
                true
            )
        }

        val width = builder.width
        val height = builder.height.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        builder.draw(canvas)

        return bitmap
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
