package com.example.surjomukhi

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    private val context = application.applicationContext

    // Init SyncEngine
    val syncEngine = SyncEngine(context)

    // Language Preference: true = Bangla (বাংলা), false = English
    val isBangla = MutableStateFlow(
        context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_bangla_lang", true)
    )

    // Expose flows from SyncEngine
    val myPhone = syncEngine.myPhone
    val myNickname = syncEngine.myNickname
    val myBindingCode = syncEngine.myBindingCode
    val partnerPhone = syncEngine.partnerPhone
    val partnerNickname = syncEngine.partnerNickname

    val myStatus = syncEngine.myStatus
    val myCustomText = syncEngine.myCustomText
    val myUpdatedAt = syncEngine.myUpdatedAt

    val partnerStatus = syncEngine.partnerStatus
    val partnerCustomText = syncEngine.partnerCustomText
    val partnerUpdatedAt = syncEngine.partnerUpdatedAt

    val myMuteUntil = syncEngine.myMuteUntil
    val myMuteMessage = syncEngine.myMuteMessage
    val partnerMuteUntil = syncEngine.partnerMuteUntil
    val partnerMuteMessage = syncEngine.partnerMuteMessage
    val autoMuteUntil = syncEngine.autoMuteUntil
    val consecutiveVibesSent = syncEngine.consecutiveVibesSent

    // Expose functional settings flows
    val isVibrationEnabled = syncEngine.isVibrationEnabled
    val isNotificationEnabled = syncEngine.isNotificationEnabled
    val isBackgroundSyncEnabled = syncEngine.isBackgroundSyncEnabled
    val vibrationDurationMs = syncEngine.vibrationDurationMs

    // Chirkut (চিরকুট) Custom Message Overlay flows
    val myChirkut = syncEngine.myChirkut
    val partnerChirkut = syncEngine.partnerChirkut
    val isChirkutEnabled = syncEngine.isChirkutEnabled

    // GitHub Update Manager
    val gitHubUpdateManager = GitHubUpdateManager(context)
    val gitHubRepo = gitHubUpdateManager.githubRepo
    val isCheckingGitHubUpdate = gitHubUpdateManager.isChecking
    val isGitHubUpdateAvailable = gitHubUpdateManager.isUpdateAvailable
    val latestGitHubRelease = gitHubUpdateManager.latestRelease
    val gitHubStatusMessage = gitHubUpdateManager.statusMessage

    fun checkForGitHubUpdates() {
        viewModelScope.launch {
            gitHubUpdateManager.checkForUpdates(com.example.BuildConfig.VERSION_NAME)
        }
    }

    fun updateGitHubRepoSlug(slug: String) {
        gitHubUpdateManager.updateRepoSlug(slug)
        checkForGitHubUpdates()
    }

    fun dismissGitHubUpdate() {
        gitHubUpdateManager.dismissUpdate()
    }

    fun sendChirkut(message: String) {
        syncEngine.updateChirkut(message)
        addLog(if (message.isNotEmpty()) "একটি চিরকুট পাঠানো হয়েছে: \"$message\"" else "চিরকুট মুছে ফেলা হয়েছে।")
        triggerWidgetUpdate()
    }

    fun toggleChirkut(enabled: Boolean) {
        syncEngine.toggleChirkutEnabled(enabled)
        addLog(if (enabled) "উইজেটে চিরকুট প্রদর্শন চালু করা হয়েছে।" else "উইজেটে চিরকুট প্রদর্শন বন্ধ করা হয়েছে।")
        triggerWidgetUpdate()
    }

    // UI Feedback state when vibration received
    private val _vibrationFeedbackActive = MutableStateFlow(false)
    val vibrationFeedbackActive: StateFlow<Boolean> = _vibrationFeedbackActive.asStateFlow()

    // Status of last ping (e.g. "Triggered Vibe", "Blocked", etc.)
    private val _lastPingStatus = MutableStateFlow<String?>(null)
    val lastPingStatus: StateFlow<String?> = _lastPingStatus.asStateFlow()

    // Activity trace logger for emotional connection tracking
    val activityLogs = mutableStateListOf<String>()

    init {
        addLog("সূর্যমুখী সংযোগ চালু হয়েছে। (Surjomukhi Connection Started)")
        if (syncEngine.isFirestoreEnabled) {
            addLog("সংযোগ স্ট্যাটাস: ফায়ারস্টোর ক্লাউড সিঙ্ক সক্রিয় রয়েছে।")
        } else {
            addLog("লোকাল ব্যাকআপ ডাটাবেস সক্রিয় রয়েছে।")
        }

        // Keep widget updated with the last cached statuses
        triggerWidgetUpdate()

        // Observe incoming vibration events to trigger device physical vibrator
        viewModelScope.launch {
            syncEngine.vibrationEvents.collect { event ->
                handleIncomingVibrationEvent(event)
            }
        }

        // Check for GitHub OTA updates on app launch
        checkForGitHubUpdates()
    }

    private var verificationId: String? = null
    private var forceResendingToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null

    private fun addLog(message: String) {
        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        activityLogs.add(0, "[$timeStr] $message")
        if (activityLogs.size > 15) {
            activityLogs.removeAt(activityLogs.lastIndex)
        }
    }

    fun toggleLanguage() {
        isBangla.value = !isBangla.value
        context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_bangla_lang", isBangla.value)
            .apply()
    }

    fun signUpWithPassword(
        target: String,
        password: String,
        isEmail: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanTarget = target.trim()
        if (isEmail) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanTarget).matches()) {
                onResult(false, if (isBangla.value) "সঠিক ইমেইল অ্যাড্রেস লিখুন!" else "Please enter a valid email address!")
                return
            }
        } else {
            val rawPhone = cleanTarget.removePrefix("+880").removePrefix("880").removePrefix("0")
            if (rawPhone.length < 10) {
                onResult(false, if (isBangla.value) "নম্বরটি খুব ছোট! সঠিক নম্বর টাইপ করুন।" else "Phone number is too short!")
                return
            }
        }

        if (password.length < 4) {
            onResult(false, if (isBangla.value) "পাসওয়ার্ডটি অন্তত ৪ অক্ষরের হতে হবে!" else "Password must be at least 4 characters!")
            return
        }

        val displayTarget = if (isEmail) cleanTarget else (if (cleanTarget.startsWith("+")) cleanTarget else "+880${cleanTarget.removePrefix("0").trim()}" )

        if (syncEngine.isFirestoreEnabled) {
            val dbRef = syncEngine.db
            dbRef?.collection("users")?.document(displayTarget)?.get()
                ?.addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        onResult(false, if (isBangla.value) "এই নম্বর/ইমেইল দিয়ে ইতিমধ্যে অ্যাকাউন্ট খোলা আছে!" else "Account already exists with this phone/email!")
                    } else {
                        val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                        val myCode = numericHash.toString()
                        
                        val userMap = mapOf(
                            "phone" to displayTarget,
                            "password" to password,
                            "nickname" to "NICKNAME_NOT_SET",
                            "bindingCode" to myCode,
                            "boundPartnerPhone" to "",
                            "status" to "free",
                            "customText" to "",
                            "updatedAt" to System.currentTimeMillis(),
                            "vibeMuteUntil" to 0L,
                            "vibeMuteMessage" to ""
                        )
                        
                        dbRef.collection("users").document(displayTarget).set(userMap)
                            .addOnSuccessListener {
                                val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putString("local_password_$displayTarget", password).apply()

                                syncEngine.loginUser(
                                    phone = displayTarget,
                                    nickname = "NICKNAME_NOT_SET",
                                    bindingCode = myCode
                                )
                                addLog(if (isBangla.value) "নিবন্ধন সফল হয়েছে! স্বাগতম সূর্যমুখীতে।" else "Registration successful! Welcome to Surjomukhi.")
                                triggerWidgetUpdate()
                                onResult(true, "নিবন্ধন সফল হয়েছে!")
                            }
                            .addOnFailureListener { e ->
                                onResult(false, "ত্রুটি: ${e.localizedMessage}")
                            }
                    }
                }
                ?.addOnFailureListener { e ->
                    // Fallback to local offline registration
                    val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
                    val existingPass = prefs.getString("local_password_$displayTarget", null)
                    if (existingPass != null) {
                        onResult(false, if (isBangla.value) "এই নম্বর/ইমেইল দিয়ে ইতিমধ্যে অ্যাকাউন্ট খোলা আছে!" else "Account already exists with this phone/email!")
                    } else {
                        val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                        val myCode = numericHash.toString()
                        prefs.edit().putString("local_password_$displayTarget", password).apply()
                        syncEngine.loginUser(
                            phone = displayTarget,
                            nickname = "NICKNAME_NOT_SET",
                            bindingCode = myCode
                        )
                        addLog(if (isBangla.value) "লোকাল নিবন্ধন সফল হয়েছে (অফলাইন মোড)।" else "Local offline registration successful.")
                        triggerWidgetUpdate()
                        onResult(true, "নিবন্ধন সফল হয়েছে!")
                    }
                }
        } else {
            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            val existingPass = prefs.getString("local_password_$displayTarget", null)
            if (existingPass != null) {
                onResult(false, if (isBangla.value) "এই নম্বর/ইমেইল দিয়ে ইতিমধ্যে অ্যাকাউন্ট খোলা আছে!" else "Account already exists with this phone/email!")
            } else {
                val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                val myCode = numericHash.toString()
                prefs.edit().putString("local_password_$displayTarget", password).apply()
                syncEngine.loginUser(
                    phone = displayTarget,
                    nickname = "NICKNAME_NOT_SET",
                    bindingCode = myCode
                )
                addLog(if (isBangla.value) "লোকাল নিবন্ধন সফল হয়েছে (অফলাইন মোড)।" else "Local offline registration successful.")
                triggerWidgetUpdate()
                onResult(true, "নিবন্ধন সফল হয়েছে!")
            }
        }
    }

    fun loginWithPassword(
        target: String,
        password: String,
        isEmail: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanTarget = target.trim()
        if (isEmail) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanTarget).matches()) {
                onResult(false, if (isBangla.value) "সঠিক ইমেইল অ্যাড্রেস লিখুন!" else "Please enter a valid email address!")
                return
            }
        } else {
            val rawPhone = cleanTarget.removePrefix("+880").removePrefix("880").removePrefix("0")
            if (rawPhone.length < 10) {
                onResult(false, if (isBangla.value) "নম্বরটি খুব ছোট! সঠিক নম্বর টাইপ করুন।" else "Phone number is too short!")
                return
            }
        }

        if (password.isEmpty()) {
            onResult(false, if (isBangla.value) "পাসওয়ার্ড লিখুন!" else "Please enter your password!")
            return
        }

        val displayTarget = if (isEmail) cleanTarget else (if (cleanTarget.startsWith("+")) cleanTarget else "+880${cleanTarget.removePrefix("0").trim()}" )

        if (syncEngine.isFirestoreEnabled) {
            val dbRef = syncEngine.db
            dbRef?.collection("users")?.document(displayTarget)?.get()
                ?.addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        val dbPassword = snapshot.getString("password") ?: ""
                        if (dbPassword == password) {
                            val dbNick = snapshot.getString("nickname") ?: "NICKNAME_NOT_SET"
                            val dbBindingCode = snapshot.getString("bindingCode") ?: ""
                            val boundPartner = snapshot.getString("boundPartnerPhone") ?: ""
                            
                            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("local_password_$displayTarget", password).apply()

                            if (boundPartner.isNotEmpty()) {
                                syncEngine.partnerPhone.value = boundPartner
                            }

                            syncEngine.loginUser(
                                phone = displayTarget,
                                nickname = dbNick,
                                bindingCode = dbBindingCode
                            )
                            
                            addLog(if (isBangla.value) "সফলভাবে লগইন করা হয়েছে।" else "Logged in successfully.")
                            triggerWidgetUpdate()
                            onResult(true, "লগইন সফল!")
                        } else {
                            onResult(false, if (isBangla.value) "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।" else "Incorrect password! Please try again.")
                        }
                    } else {
                        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
                        val localPass = prefs.getString("local_password_$displayTarget", null)
                        if (localPass != null) {
                            if (localPass == password) {
                                val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                                val myCode = numericHash.toString()
                                syncEngine.loginUser(
                                    phone = displayTarget,
                                    nickname = "NICKNAME_NOT_SET",
                                    bindingCode = myCode
                                )
                                onResult(true, "লগইন সফল!")
                            } else {
                                onResult(false, if (isBangla.value) "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।" else "Incorrect password! Please try again.")
                            }
                        } else {
                            onResult(false, if (isBangla.value) "অ্যাকাউন্ট খুঁজে পাওয়া যায়নি! প্রথমে সাইন-আপ করুন।" else "Account not found! Please register first.")
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
                    val localPass = prefs.getString("local_password_$displayTarget", null)
                    if (localPass != null) {
                        if (localPass == password) {
                            val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                            val myCode = numericHash.toString()
                            syncEngine.loginUser(
                                phone = displayTarget,
                                nickname = "NICKNAME_NOT_SET",
                                bindingCode = myCode
                            )
                            onResult(true, "লগইন সফল (অফলাইন মোড)!")
                        } else {
                            onResult(false, if (isBangla.value) "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।" else "Incorrect password! Please try again.")
                        }
                    } else {
                        onResult(false, if (isBangla.value) "অফলাইন: অ্যাকাউন্ট খুঁজে পাওয়া যায়নি!" else "Offline: Account not found!")
                    }
                }
        } else {
            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            val localPass = prefs.getString("local_password_$displayTarget", null)
            if (localPass != null) {
                if (localPass == password) {
                    val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                    val myCode = numericHash.toString()
                    syncEngine.loginUser(
                        phone = displayTarget,
                        nickname = "NICKNAME_NOT_SET",
                        bindingCode = myCode
                    )
                    onResult(true, "লগইন সফল!")
                } else {
                    onResult(false, if (isBangla.value) "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।" else "Incorrect password! Please try again.")
                }
            } else {
                onResult(false, if (isBangla.value) "অ্যাকাউন্ট খুঁজে পাওয়া যায়নি! প্রথমে সাইন-আপ করুন।" else "Account not found! Please register first.")
            }
        }
    }

    fun updateMyNickname(newNickname: String) {
        val cleanNick = newNickname.trim()
        if (cleanNick.isNotEmpty()) {
            syncEngine.myNickname.value = cleanNick
            syncEngine.saveLocalPreferences()
            if (syncEngine.isFirestoreEnabled && myPhone.value.isNotEmpty()) {
                syncEngine.db?.collection("users")?.document(myPhone.value)?.update("nickname", cleanNick)
            }
            addLog(if (isBangla.value) "নিকনেম পরিবর্তন করে \"$cleanNick\" রাখা হয়েছে।" else "Nickname updated to \"$cleanNick\".")
            triggerWidgetUpdate()
        }
    }

    // RE-BIND MANUALLY WITH 6-DIGIT CODE
    fun bindPartner(code: String, onResult: (Boolean, String) -> Unit) {
        syncEngine.bindWithPartnerCode(
            code = code,
            onSuccess = {
                addLog("অভিনন্দন! আপনার সংযোগ সফলভাবে স্থাপিত হয়েছে!")
                triggerWidgetUpdate()
                onResult(true, "সংযুক্ত হয়েছে!")
            },
            onFailure = { error ->
                addLog("সংযোগ ব্যর্থ: $error")
                onResult(false, error)
            }
        )
    }

    // UPDATE STATUS
    fun setUserStatus(status: StatusType, customText: String = "") {
        syncEngine.updateMyStatus(status.key, customText)
        
        val statusLabel = if (isBangla.value) status.labelBn else status.labelEn
        addLog("আপনি নিজের মনের ভাব পরিবর্তন করেছেন: \"$statusLabel\"" + 
            if (customText.isNotEmpty()) " ($customText)" else "")

        triggerWidgetUpdate()
    }

    // UPDATE NICKNAMES (Self or Partner)
    fun updateProfileNicknames(myNewNick: String, partnerNewNick: String) {
        syncEngine.updateNicknames(myNewNick.trim(), partnerNewNick.trim())
        addLog("নিকনেম আপডেট করা হয়েছে: নিজেরটা \"$myNewNick\", পার্টনার \"$partnerNewNick\"")
        triggerWidgetUpdate()
    }

    // SEND PING (VIBRATION PULSE)
    fun sendPingNotification() {
        val partner = partnerNickname.value
        val isBn = isBangla.value
        
        _lastPingStatus.value = "Sending..."
        
        val result = syncEngine.sendVibrationPing()
        when (result) {
            "SUCCESS" -> {
                _lastPingStatus.value = if (isBn) "সফলভাবে ভাইব্রেশন পাঠানো হয়েছে!" else "Vibe sent successfully!"
                addLog(if (isBn) "\"$partner\" এর মনের সংযোগে ভাইব্রেশন পাঠানো হয়েছে..." else "Vibe sent successfully to \"$partner\"...")
            }
            "MUTED" -> {
                val muteMsg = partnerMuteMessage.value
                val cleanMuteMsg = if (muteMsg.isNotEmpty()) " (বার্তা: $muteMsg)" else ""
                _lastPingStatus.value = if (isBn) "পার্টনার মিউট করে রেখেছেন" else "Partner has muted vibes"
                addLog(if (isBn) "🚫 ভাইব্রেশন বাতিল: \"$partner\" আপনাকে মিউট করে রেখেছেন$cleanMuteMsg।" else "🚫 Vibe Cancelled: \"$partner\" has muted you$cleanMuteMsg.")
            }
            "AUTOMUTED" -> {
                _lastPingStatus.value = if (isBn) "টানা ৩ বার নো-রেসপন্সে লকড" else "Locked due to 3 consecutive no-responses"
                addLog(if (isBn) "⚠️ অটো-মিউট সচল: টানা ৩ বার ভাইব্রেশনের কোনো উত্তর না পাওয়ায় ভাইব্রেশন সিঙ্ক আধা ঘণ্টার জন্য বন্ধ রয়েছে।" else "⚠️ Auto-Mute Active: No response after 3 vibes. Sync disabled for 30 minutes.")
            }
            "AUTOMUTED_TRIGGERED" -> {
                _lastPingStatus.value = if (isBn) "অটো-মিউট চালু করা হলো" else "Auto-mute triggered"
                addLog(if (isBn) "⚠️ আপনি টানা ৩ বার ভাইব্রেশন পাঠিয়েছেন কিন্তু কোনো উত্তর পাননি। তাই ভাইব্রেশন সিঙ্ক আধা ঘণ্টার জন্য লক করা হলো।" else "⚠️ You sent 3 vibes with no response. Auto-muting sync for 30 minutes.")
            }
            "NO_CONNECTION" -> {
                _lastPingStatus.value = if (isBn) "সংযোগ পাওয়া যায়নি" else "No connection"
                addLog(if (isBn) "❌ ভাইব্রেশন সিগন্যাল পাঠানো যায়নি। সংযোগ চেক করুন।" else "❌ Vibe sending failed. Please check connection.")
            }
            else -> {
                _lastPingStatus.value = if (isBn) "সমস্যা হয়েছে" else "Error occurred"
            }
        }
    }

    // Set mute status
    fun updateMuteStatus(minutes: Int, message: String) {
        val isBn = isBangla.value
        syncEngine.updateMyMuteStatus(minutes, message)
        if (minutes > 0) {
            addLog(if (isBn) "🔇 ভাইব্রেশন মিউট করা হয়েছে $minutes মিনিটের জন্য।" else "🔇 Vibes muted for $minutes minutes.")
        } else {
            addLog(if (isBn) "🔊 ভাইব্রেশন মিউট বন্ধ করা হয়েছে।" else "🔊 Vibes unmuted.")
        }
        triggerWidgetUpdate()
    }

    // Settings modifiers
    fun setVibrationEnabled(enabled: Boolean) {
        syncEngine.isVibrationEnabled.value = enabled
        syncEngine.saveLocalPreferences()
        val isBn = isBangla.value
        addLog(if (isBn) "ভাইব্রেশন স্পর্শ: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"}" else "Vibration touch: ${if (enabled) "Enabled" else "Disabled"}")
    }

    fun setNotificationEnabled(enabled: Boolean) {
        syncEngine.isNotificationEnabled.value = enabled
        syncEngine.saveLocalPreferences()
        val isBn = isBangla.value
        addLog(if (isBn) "বার্তা নোটিফিকেশন: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"}" else "Message Notification: ${if (enabled) "Enabled" else "Disabled"}")
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        syncEngine.isBackgroundSyncEnabled.value = enabled
        syncEngine.saveLocalPreferences()
        
        try {
            val serviceIntent = Intent(context, SurjomukhiSyncService::class.java)
            if (enabled) {
                context.startService(serviceIntent)
                com.example.surjomukhi.SurjomukhiAlarmReceiver.scheduleNextAlarm(context)
            } else {
                context.stopService(serviceIntent)
                com.example.surjomukhi.SurjomukhiAlarmReceiver.stopAlarm(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling sync service/alarm state", e)
        }

        val isBn = isBangla.value
        addLog(if (isBn) "ব্যাকগ্রাউন্ড অলটাইম সিঙ্ক: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"}" else "Background All-time Sync: ${if (enabled) "Enabled" else "Disabled"}")
    }

    fun setVibrationDuration(durationMs: Long) {
        syncEngine.vibrationDurationMs.value = durationMs
        syncEngine.saveLocalPreferences()
        val isBn = isBangla.value
        val label = when (durationMs) {
            200L -> if (isBn) "সংক্ষিপ্ত (২০০ মিলি সেকেন্ড)" else "Short (200ms)"
            800L -> if (isBn) "দীর্ঘ (৮০০ মিলি সেকেন্ড)" else "Long (800ms)"
            else -> if (isBn) "মধ্যম (৪৫০ মিলি সেকেন্ড)" else "Medium (450ms)"
        }
        addLog(if (isBn) "ভাইব্রেশন স্পর্শের সময়কাল: $label" else "Vibration duration: $label")
    }

    // DETECT INCOMING VIBRATIONS (Trigger real device vibrator conditionally)
    private fun handleIncomingVibrationEvent(event: VibrationEvent) {
        // Ensure this event was addressed to me
        if (event.to == myPhone.value) {
            val senderLabel = partnerNickname.value
            val currentStatus = StatusType.fromKey(myStatus.value)

            if (currentStatus == StatusType.FREE) {
                // Trigger physical vibration if enabled
                val duration = vibrationDurationMs.value
                if (isVibrationEnabled.value) {
                    triggerDeviceVibrator(duration)
                }
                _vibrationFeedbackActive.value = true
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _vibrationFeedbackActive.value = false
                }
                
                _lastPingStatus.value = "Success: Ping Triggered!"
                val isBn = isBangla.value
                val durationLabel = when (duration) {
                    200L -> if (isBn) "২০০ মিলি সেকেন্ড" else "200ms"
                    800L -> if (isBn) "৮০০ মিলি সেকেন্ড" else "800ms"
                    else -> if (isBn) "৪৫০ মিলি সেকেন্ড" else "450ms"
                }
                addLog("🔔 মনের সংযোগ! \"$senderLabel\" এর অনুভূতিতে আপনার ফোন কেঁপে উঠেছে ($durationLabel)।")
            } else {
                _lastPingStatus.value = "Ignored: Target is ${currentStatus.labelEn}"
                addLog("🚫 ভাইব্রেশন বাতিল: \"$senderLabel\" আপনাকে স্পর্শ করতে চেয়েছিল, কিন্তু আপনি অন্য কাজে ব্যস্ত [অবস্থা: ${currentStatus.labelBn}]।")
            }
        }
    }

    private fun triggerDeviceVibrator(durationMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate device hardware: ${e.message}")
        }
    }

    fun logout() {
        syncEngine.logout()
        addLog("প্রস্থান করা হয়েছে। নতুন সেশন বা সংযোগ সেট করুন।")
        triggerWidgetUpdate()
    }

    fun triggerWidgetUpdate() {
        // Cache states inside SharedPreferences for AppWidgetProvider
        val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("my_phone", myPhone.value)
            .putString("my_nickname", myNickname.value)
            .putString("partner_phone", partnerPhone.value)
            .putString("partner_nickname", partnerNickname.value)
            .putString("my_status", myStatus.value)
            .putString("my_custom_text", myCustomText.value)
            .putString("partner_status", partnerStatus.value)
            .putString("partner_custom_text", partnerCustomText.value)
            .putString("my_chirkut", myChirkut.value)
            .putString("partner_chirkut", partnerChirkut.value)
            .putBoolean("is_chirkut_enabled", isChirkutEnabled.value)
            .apply()

        // Notify Widget immediately & instantly without waiting for broadcast queue
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, com.example.surjomukhi.widget.SurjomukhiWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
            val provider = com.example.surjomukhi.widget.SurjomukhiWidgetProvider()
            for (id in ids) {
                provider.updateWidgetContent(context, appWidgetManager, id)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to execute immediate widget refresh from ViewModel", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.shutdown()
    }
}
