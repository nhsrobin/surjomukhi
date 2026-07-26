package com.example.surjomukhi

import com.example.MainActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SyncEngine(private val context: Context) {
    private val TAG = "SyncEngine"

    var isFirestoreEnabled = false
        private set

    var db: FirebaseFirestore? = null
        private set

    // Persistent login states
    val myPhone = MutableStateFlow("")
    val myNickname = MutableStateFlow("")
    val myBindingCode = MutableStateFlow("")
    val partnerPhone = MutableStateFlow("")
    val partnerNickname = MutableStateFlow("")

    // Status states
    val myStatus = MutableStateFlow("free")
    val myCustomText = MutableStateFlow("")
    val myUpdatedAt = MutableStateFlow(System.currentTimeMillis())

    val partnerStatus = MutableStateFlow("free")
    val partnerCustomText = MutableStateFlow("")
    val partnerUpdatedAt = MutableStateFlow(System.currentTimeMillis())

    // Vibe Mute / Auto Mute properties
    val myMuteUntil = MutableStateFlow(0L)
    val myMuteMessage = MutableStateFlow("")
    val partnerMuteUntil = MutableStateFlow(0L)
    val partnerMuteMessage = MutableStateFlow("")
    val autoMuteUntil = MutableStateFlow(0L)
    val consecutiveVibesSent = MutableStateFlow(0)
    val partnerLastKnownUpdatedAt = MutableStateFlow(0L)

    // Functional Settings Options
    val isVibrationEnabled = MutableStateFlow(true)
    val isNotificationEnabled = MutableStateFlow(true)
    val isBackgroundSyncEnabled = MutableStateFlow(true)
    val vibrationDurationMs = MutableStateFlow(450L)

    // Chirkut (চিরকুট) Custom Message Overlay States
    val myChirkut = MutableStateFlow("")
    val partnerChirkut = MutableStateFlow("")
    val isChirkutEnabled = MutableStateFlow(true)

    // Direct event channel for vibrations
    private val _vibrationEvents = MutableSharedFlow<VibrationEvent>(0, 64)
    val vibrationEvents: SharedFlow<VibrationEvent> = _vibrationEvents

    // Firestore Listener Registrations
    private var myDocListener: ListenerRegistration? = null
    private var partnerDocListener: ListenerRegistration? = null
    private var vibrationListener: ListenerRegistration? = null

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            "my_status" -> {
                val newValue = prefs.getString("my_status", "free") ?: "free"
                if (myStatus.value != newValue) {
                    myStatus.value = newValue
                }
            }
            "my_custom_text" -> {
                val newValue = prefs.getString("my_custom_text", "") ?: ""
                if (myCustomText.value != newValue) {
                    myCustomText.value = newValue
                }
            }
            "partner_status" -> {
                val newValue = prefs.getString("partner_status", "free") ?: "free"
                if (partnerStatus.value != newValue) {
                    partnerStatus.value = newValue
                }
            }
            "partner_custom_text" -> {
                val newValue = prefs.getString("partner_custom_text", "") ?: ""
                if (partnerCustomText.value != newValue) {
                    partnerCustomText.value = newValue
                }
            }
            "my_nickname" -> {
                val newValue = prefs.getString("my_nickname", "আমার মন") ?: "আমার মন"
                if (myNickname.value != newValue) {
                    myNickname.value = newValue
                }
            }
            "partner_nickname" -> {
                val newValue = prefs.getString("partner_nickname", "কল্পনা") ?: "কল্পনা"
                if (partnerNickname.value != newValue) {
                    partnerNickname.value = newValue
                }
            }
            "partner_phone" -> {
                val newValue = prefs.getString("partner_phone", "") ?: ""
                if (partnerPhone.value != newValue) {
                    partnerPhone.value = newValue
                    startSyncListeners()
                }
            }
            "my_chirkut" -> {
                val newValue = prefs.getString("my_chirkut", "") ?: ""
                if (myChirkut.value != newValue) {
                    myChirkut.value = newValue
                }
            }
            "partner_chirkut" -> {
                val newValue = prefs.getString("partner_chirkut", "") ?: ""
                if (partnerChirkut.value != newValue) {
                    partnerChirkut.value = newValue
                }
            }
            "is_chirkut_enabled" -> {
                val newValue = prefs.getBoolean("is_chirkut_enabled", true)
                if (isChirkutEnabled.value != newValue) {
                    isChirkutEnabled.value = newValue
                }
            }
        }
    }

    init {
        // Load persistent local configurations
        loadLocalPreferences()
        initializeFirebase()

        // Register shared preference change listener
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private fun loadLocalPreferences() {
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        myPhone.value = prefs.getString("my_phone", "") ?: ""
        myNickname.value = prefs.getString("my_nickname", "আমার মন") ?: "আমার মন"
        myBindingCode.value = prefs.getString("my_binding_code", "") ?: ""
        partnerPhone.value = prefs.getString("partner_phone", "") ?: ""
        partnerNickname.value = prefs.getString("partner_nickname", "কল্পনা") ?: "কল্পনা"
        myStatus.value = prefs.getString("my_status", "free") ?: "free"
        myCustomText.value = prefs.getString("my_custom_text", "") ?: ""
        partnerStatus.value = prefs.getString("partner_status", "free") ?: "free"
        partnerCustomText.value = prefs.getString("partner_custom_text", "") ?: ""
        myMuteUntil.value = prefs.getLong("my_mute_until", 0L)
        myMuteMessage.value = prefs.getString("my_mute_message", "") ?: ""
        partnerMuteUntil.value = prefs.getLong("partner_mute_until", 0L)
        partnerMuteMessage.value = prefs.getString("partner_mute_message", "") ?: ""
        autoMuteUntil.value = prefs.getLong("auto_mute_until", 0L)
        consecutiveVibesSent.value = prefs.getInt("consecutive_vibes_sent", 0)
        partnerLastKnownUpdatedAt.value = prefs.getLong("partner_last_known_updated_at", 0L)
        isVibrationEnabled.value = prefs.getBoolean("is_vibration_enabled", true)
        isNotificationEnabled.value = prefs.getBoolean("is_notification_enabled", true)
        isBackgroundSyncEnabled.value = prefs.getBoolean("is_background_sync_enabled", true)
        vibrationDurationMs.value = prefs.getLong("vibration_duration_ms", 450L)
        myChirkut.value = prefs.getString("my_chirkut", "") ?: ""
        partnerChirkut.value = prefs.getString("partner_chirkut", "") ?: ""
        isChirkutEnabled.value = prefs.getBoolean("is_chirkut_enabled", true)
    }

    fun saveLocalPreferences() {
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("my_phone", myPhone.value)
            .putString("my_nickname", myNickname.value)
            .putString("my_binding_code", myBindingCode.value)
            .putString("partner_phone", partnerPhone.value)
            .putString("partner_nickname", partnerNickname.value)
            .putString("my_status", myStatus.value)
            .putString("my_custom_text", myCustomText.value)
            .putString("partner_status", partnerStatus.value)
            .putString("partner_custom_text", partnerCustomText.value)
            .putLong("my_mute_until", myMuteUntil.value)
            .putString("my_mute_message", myMuteMessage.value)
            .putLong("partner_mute_until", partnerMuteUntil.value)
            .putString("partner_mute_message", partnerMuteMessage.value)
            .putLong("auto_mute_until", autoMuteUntil.value)
            .putInt("consecutive_vibes_sent", consecutiveVibesSent.value)
            .putLong("partner_last_known_updated_at", partnerLastKnownUpdatedAt.value)
            .putBoolean("is_vibration_enabled", isVibrationEnabled.value)
            .putBoolean("is_notification_enabled", isNotificationEnabled.value)
            .putBoolean("is_background_sync_enabled", isBackgroundSyncEnabled.value)
            .putLong("vibration_duration_ms", vibrationDurationMs.value)
            .putString("my_chirkut", myChirkut.value)
            .putString("partner_chirkut", partnerChirkut.value)
            .putBoolean("is_chirkut_enabled", isChirkutEnabled.value)
            .apply()

        // Sync with Widget immediately & instantly without waiting for broadcast queue
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, com.example.surjomukhi.widget.SurjomukhiWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
            val provider = com.example.surjomukhi.widget.SurjomukhiWidgetProvider()
            for (id in ids) {
                provider.updateWidgetContent(context, appWidgetManager, id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute immediate widget refresh from SyncEngine", e)
        }
    }

    private fun initializeFirebase() {
        try {
            // === আপনার ফায়ারবেস কনফিগারেশন এখানে পরিবর্তন করুন ===
            // নিচে দেওয়া ভ্যালুগুলো আপনার ফায়ারবেস প্রজেক্টের সাথে পরিবর্তন করে নিন।
            // ফায়ারবেস প্রজেক্ট সেটিংস (Project Settings) থেকে এগুলো পেয়ে যাবেন।
            val optionsBuilder = FirebaseOptions.Builder()
                .setProjectId("surjojmukhi-app") // আপনার ফায়ারবেস Project ID
                .setApplicationId("1:592810166563:android:e0325aa3502bb40963e496") // আপনার ফায়ারবেস App ID
                .setApiKey("AIzaSyCEdgAtPmZGWlEEldgcaiuq7Mx5Yb1OCdY") // আপনার ফায়ারবেস Web API Key
            // =======================================================

            val firebaseApp = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context, optionsBuilder.build())
            } else {
                FirebaseApp.getInstance()
            }

            db = FirebaseFirestore.getInstance(firebaseApp)
            isFirestoreEnabled = true
            Log.d(TAG, "SyncEngine: Firestore Connected successfully!")
            
            // Start sync listeners if logged in
            if (myPhone.value.isNotEmpty()) {
                startSyncListeners()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncEngine: Connection failed. Operating in offline local cache mode.", e)
            isFirestoreEnabled = false
        }
    }

    fun loginUser(phone: String, nickname: String, bindingCode: String) {
        myPhone.value = phone
        myNickname.value = nickname.ifBlank { "আমার মন" }
        myBindingCode.value = bindingCode
        saveLocalPreferences()

        if (isFirestoreEnabled) {
            // Push or fetch user profile on Firestore
            val userMap = mapOf(
                "phone" to phone,
                "nickname" to myNickname.value,
                "bindingCode" to bindingCode,
                "boundPartnerPhone" to partnerPhone.value,
                "status" to myStatus.value,
                "customText" to myCustomText.value,
                "updatedAt" to System.currentTimeMillis(),
                "vibeMuteUntil" to myMuteUntil.value,
                "vibeMuteMessage" to myMuteMessage.value
            )
            db?.collection("users")?.document(phone)?.set(userMap)
            startSyncListeners()
        }
    }

    fun startSyncListeners() {
        shutdownListeners()
        val phone = myPhone.value
        if (phone.isEmpty() || !isFirestoreEnabled) return

        try {
            // 1. Listen to MY document (to detect binding or nickname changes)
            myDocListener = db?.collection("users")?.document(phone)
                ?.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen self failed", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val boundPhone = snapshot.getString("boundPartnerPhone") ?: ""
                        val selfNickname = snapshot.getString("nickname") ?: "আমার মন"
                        val selfChirkut = snapshot.getString("chirkut") ?: ""
                        
                        myNickname.value = selfNickname
                        myChirkut.value = selfChirkut
                        
                        if (partnerPhone.value != boundPhone) {
                            partnerPhone.value = boundPhone
                            saveLocalPreferences()
                            // Re-start listeners to include partner doc
                            startSyncListeners()
                        }
                    }
                }

            // 2. Listen to PARTNER document (to get their realtime status & nickname)
            val partner = partnerPhone.value
            if (partner.isNotEmpty()) {
                partnerDocListener = db?.collection("users")?.document(partner)
                    ?.addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w(TAG, "Listen partner failed", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            partnerNickname.value = snapshot.getString("nickname") ?: "কল্পনা"
                            partnerStatus.value = snapshot.getString("status") ?: "free"
                            partnerCustomText.value = snapshot.getString("customText") ?: ""
                            partnerChirkut.value = snapshot.getString("chirkut") ?: ""
                            
                            val newUpdatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                            partnerMuteUntil.value = snapshot.getLong("vibeMuteUntil") ?: 0L
                            partnerMuteMessage.value = snapshot.getString("vibeMuteMessage") ?: ""
                            
                            if (newUpdatedAt > partnerLastKnownUpdatedAt.value) {
                                consecutiveVibesSent.value = 0
                                autoMuteUntil.value = 0L // reset auto-mute since partner had active response
                                partnerLastKnownUpdatedAt.value = newUpdatedAt
                            }
                            partnerUpdatedAt.value = newUpdatedAt
                            saveLocalPreferences()
                        }
                    }
            }

            // 3. Listen to incoming vibration pings directed to me (Index-free, client-side filtered for resilience)
            vibrationListener = db?.collection("vibrations")
                ?.whereEqualTo("toPhone", phone)
                ?.addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen vibration events failed", error)
                        return@addSnapshotListener
                    }
                    if (querySnapshot != null) {
                        val fifteenSecsAgo = System.currentTimeMillis() - 15_000
                        for (doc in querySnapshot.documentChanges) {
                            if (doc.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) continue
                            val changeDoc = doc.document
                            val timestamp = changeDoc.getLong("timestamp") ?: 0
                            if (timestamp < fifteenSecsAgo) continue

                            val fromPhone = changeDoc.getString("fromPhone") ?: ""
                            
                            // Reset consecutive vibes sent because partner interacted/responded
                            consecutiveVibesSent.value = 0
                            autoMuteUntil.value = 0L
                            saveLocalPreferences()

                            // Check if muted
                            val isMuted = myMuteUntil.value > System.currentTimeMillis()

                            if (!isMuted) {
                                showVibrationNotification(context, partnerNickname.value)
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                _vibrationEvents.emit(VibrationEvent(fromPhone, phone, timestamp))
                            }
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting sync listeners", e)
        }
    }

    // Attempt to bind with a 6-digit code
    fun bindWithPartnerCode(code: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val cleanCode = code.trim()
        if (cleanCode.length != 6) {
            onFailure("কোডটি অবশ্যই ৬ সংখ্যার হতে হবে")
            return
        }

        if (myPhone.value.isEmpty()) {
            onFailure("প্রথমে লগইন সম্পূর্ণ করুন")
            return
        }

        if (isFirestoreEnabled) {
            db?.collection("users")?.whereEqualTo("bindingCode", cleanCode)?.get()
                ?.addOnSuccessListener { querySnapshot ->
                    if (querySnapshot == null || querySnapshot.isEmpty) {
                        onFailure("ভুল কোড! কোনো পার্টনার খুঁজে পাওয়া যায়নি")
                        return@addOnSuccessListener
                    }
                    val partnerDoc = querySnapshot.documents[0]
                    val pPhone = partnerDoc.getString("phone") ?: ""
                    
                    if (pPhone == myPhone.value) {
                        onFailure("আপনি নিজের কোডে বাইন্ড করতে পারবেন না!")
                        return@addOnSuccessListener
                    }

                    // Perform mutual binding transaction
                    partnerPhone.value = pPhone
                    partnerNickname.value = partnerDoc.getString("nickname") ?: "কল্পনা"
                    saveLocalPreferences()

                    // Update my Firestore document
                    db?.collection("users")?.document(myPhone.value)
                        ?.update("boundPartnerPhone", pPhone)

                    // Update partner's Firestore document
                    db?.collection("users")?.document(pPhone)
                        ?.update("boundPartnerPhone", myPhone.value)

                    startSyncListeners()
                    onSuccess()
                }
                ?.addOnFailureListener { e ->
                    onFailure("সার্ভার সমস্যা: ${e.localizedMessage}")
                }
        } else {
            onFailure("সার্ভার সংযোগ ব্যর্থ হয়েছে। অনুগ্রহ করে ইন্টারনেট সংযোগ চেক করুন এবং পুনরায় চেষ্টা করুন।")
        }
    }

    fun updateMyStatus(statusKey: String, customText: String) {
        // Limit protection: Only write to local & Firebase if status or custom text has actually changed!
        if (myStatus.value == statusKey && myCustomText.value == customText) {
            return
        }

        val timestamp = System.currentTimeMillis()
        myStatus.value = statusKey
        myCustomText.value = customText
        myUpdatedAt.value = timestamp
        saveLocalPreferences()

        if (isFirestoreEnabled && myPhone.value.isNotEmpty()) {
            db?.collection("users")?.document(myPhone.value)?.update(
                mapOf(
                    "status" to statusKey,
                    "customText" to customText,
                    "updatedAt" to timestamp
                )
            )
        }
    }

    fun updateNicknames(myNewNick: String, partnerNewNick: String) {
        // Limit protection check
        if (myNickname.value == myNewNick && partnerNickname.value == partnerNewNick) {
            return
        }

        myNickname.value = myNewNick
        partnerNickname.value = partnerNewNick
        saveLocalPreferences()

        if (isFirestoreEnabled) {
            if (myPhone.value.isNotEmpty()) {
                db?.collection("users")?.document(myPhone.value)?.update("nickname", myNewNick)
            }
            if (partnerPhone.value.isNotEmpty()) {
                db?.collection("users")?.document(partnerPhone.value)?.update("nickname", partnerNewNick)
            }
        }
    }

    fun sendVibrationPing(): String {
        val from = myPhone.value
        val to = partnerPhone.value
        val timestamp = System.currentTimeMillis()

        if (from.isEmpty() || to.isEmpty()) return "NO_CONNECTION"

        // 1. Check if we are currently auto-muted (tana 3 vibes block for 30 mins)
        if (autoMuteUntil.value > timestamp) {
            return "AUTOMUTED"
        }

        // 2. Check if partner has muted us
        if (partnerMuteUntil.value > timestamp) {
            return "MUTED"
        }

        // 3. Track consecutive vibrations sent and check if we need to auto-mute
        // Check if partner has updated since our last vibration
        if (partnerUpdatedAt.value > partnerLastKnownUpdatedAt.value) {
            consecutiveVibesSent.value = 0
            partnerLastKnownUpdatedAt.value = partnerUpdatedAt.value
        }

        if (consecutiveVibesSent.value >= 3) {
            // Auto-mute for 30 minutes!
            autoMuteUntil.value = timestamp + 30 * 60 * 1000L
            saveLocalPreferences()
            return "AUTOMUTED_TRIGGERED"
        }

        // Increment consecutive vibes sent
        consecutiveVibesSent.value += 1
        saveLocalPreferences()

        // Local emit
        CoroutineScope(Dispatchers.Main).launch {
            _vibrationEvents.emit(VibrationEvent(from, to, timestamp))
        }

        if (isFirestoreEnabled) {
            val vMap = mapOf(
                "fromPhone" to from,
                "toPhone" to to,
                "timestamp" to timestamp
            )
            db?.collection("vibrations")?.add(vMap)
        }
        return "SUCCESS"
    }

    fun updateMyMuteStatus(muteDurationMinutes: Int, message: String) {
        val until = if (muteDurationMinutes > 0) {
            System.currentTimeMillis() + muteDurationMinutes * 60 * 1000L
        } else {
            0L
        }
        myMuteUntil.value = until
        myMuteMessage.value = message
        saveLocalPreferences()

        if (isFirestoreEnabled && myPhone.value.isNotEmpty()) {
            db?.collection("users")?.document(myPhone.value)?.update(
                mapOf(
                    "vibeMuteUntil" to until,
                    "vibeMuteMessage" to message
                )
            )
        }
    }

    fun updateChirkut(message: String) {
        myChirkut.value = message
        saveLocalPreferences()

        if (isFirestoreEnabled && myPhone.value.isNotEmpty()) {
            db?.collection("users")?.document(myPhone.value)?.update("chirkut", message)
        }
    }

    fun toggleChirkutEnabled(enabled: Boolean) {
        isChirkutEnabled.value = enabled
        saveLocalPreferences()
    }

    private fun showVibrationNotification(context: Context, senderName: String) {
        if (!isNotificationEnabled.value) {
            Log.d(TAG, "Notification skipped because they are disabled in settings")
            return
        }
        val channelId = "surjomukhi_vibe_channel"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Heart Vibrations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming heart connection vibrations"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 150, 450)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent for main activity click
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val silentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.surjomukhi.ACTION_SILENT"
            putExtra("notificationId", notificationId)
        }
        val silentPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            silentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.surjomukhi.ACTION_MUTE"
            putExtra("notificationId", notificationId)
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isBn = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_bangla_lang", true)

        val title = if (isBn) "মনের স্পর্শ! 🌻" else "Heart Connection! 🌻"
        val content = if (isBn) "\"$senderName\" আপনাকে স্পর্শ করতে চেয়েছে!" else "\"$senderName\" is trying to touch your heart!"
        val silentLabel = if (isBn) "সাইলেন্ট" else "Silent"
        val muteLabel = if (isBn) "মিউট" else "Mute"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode, silentLabel, silentPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, muteLabel, mutePendingIntent)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e("SyncEngine", "Permission missing for notification display", e)
        }
    }

    fun logout() {
        shutdownListeners()
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        myPhone.value = ""
        myNickname.value = "আমার মন"
        myBindingCode.value = ""
        partnerPhone.value = ""
        partnerNickname.value = "কল্পনা"
        myStatus.value = "free"
        myCustomText.value = ""
        partnerStatus.value = "free"
        partnerCustomText.value = ""
    }

    fun shutdownListeners() {
        myDocListener?.remove()
        partnerDocListener?.remove()
        vibrationListener?.remove()
    }

    fun shutdown() {
        try {
            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering prefListener", e)
        }
        shutdownListeners()
    }
}
