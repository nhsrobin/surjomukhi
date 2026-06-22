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
    val isBangla = MutableStateFlow(true)

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
    }

    // REAL FIREBASE PHONE NUMBER VERIFICATION (SMS OTP Verification)
    fun verifyPhoneAndSendOtp(
        activity: android.app.Activity,
        phone: String,
        nickname: String,
        onCodeSent: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val rawPhone = phone.trim()
        val cleanPhone = if (rawPhone.startsWith("+")) rawPhone else "+880${rawPhone.removePrefix("0").trim()}"

        if (cleanPhone.length < 10) {
            onFailure("নম্বরটি খুব ছোট! সঠিন নম্বর টাইপ করুন।")
            return
        }

        val callbacks = object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                // If instant verification succeeds
                val smsCode = credential.smsCode
                if (!smsCode.isNullOrEmpty()) {
                    confirmOtpAndLogin(cleanPhone, nickname, smsCode) { success, msg ->
                        // Automatically logs in
                    }
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "onVerificationFailed: Firebase Phone Auth error", e)
                val errMsg = e.localizedMessage ?: "ওটিপি পাঠাতে সমস্যা হয়েছে"
                addLog("কোড পাঠানো ব্যর্থ: $errMsg")
                onFailure(errMsg)
            }

            override fun onCodeSent(
                vId: String,
                token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vId
                forceResendingToken = token
                addLog("নিরাপদ ওটিপি কোড পাঠানো হয়েছে: $cleanPhone নাম্বারে।")
                onCodeSent()
            }
        }

        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance(com.google.firebase.FirebaseApp.getInstance())
            val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(cleanPhone)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "ফায়ারবেস কনফিগারেশন চেক করুন।"
            addLog("ত্রুটি: $errMsg")
            onFailure(errMsg)
        }
    }

    // CONFIRM REAL OTP AND PROCEED TO LOGIN
    fun confirmOtpAndLogin(phone: String, nickname: String, enteredOtp: String, onResult: (Boolean, String) -> Unit) {
        val rawPhone = phone.trim()
        val cleanPhone = if (rawPhone.startsWith("+")) rawPhone else "+880${rawPhone.removePrefix("0").trim()}"
        val cleanNickname = nickname.trim().ifEmpty { "আমার মন" }
        val vId = verificationId

        if (vId == null) {
            onResult(false, "পূর্ববর্তী ভেরিফিকেশন সেশন পাওয়া যায়নি! দয়া করে কোড পুনরায় পাঠান।")
            return
        }

        if (enteredOtp.length < 6) {
            onResult(false, "কোডটি অবশ্যই ৬ সংখ্যার হতে হবে")
            return
        }

        try {
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(vId, enteredOtp)
            com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // User is verified and authenticated fully in Firebase Authentication!
                        // Generate mutual binding code
                        val numericHash = Math.abs(cleanPhone.hashCode() % 900000) + 100000
                        val myCode = numericHash.toString()

                        // Securely register local and firestore profile session
                        syncEngine.loginUser(
                            phone = cleanPhone,
                            nickname = cleanNickname,
                            bindingCode = myCode
                        )

                        addLog("স্বাগতম $cleanNickname! ওটিপি সফলভাবে যাচাই হয়েছে। আপনি এখন একটি নিরাপদ হোস্টেড সেশনে যুক্ত।")
                        triggerWidgetUpdate()
                        onResult(true, "লগইন সফল হয়েছে!")
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "ভুল ওটিপি কোড! অনুগ্রহ করে আবার চেষ্টা করুন।"
                        onResult(false, errMsg)
                    }
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "লগইন করার সময় একটি ত্রুটি ঘটেছে।")
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
        addLog("\"$partner\" এর মনের সংযোগে ভাইব্রেশন সিগন্যাল পাঠানো হয়েছে...")
        _lastPingStatus.value = "Sending query to $partner..."

        syncEngine.sendVibrationPing()
    }

    // DETECT INCOMING VIBRATIONS (Trigger real device vibrator conditionally)
    private fun handleIncomingVibrationEvent(event: VibrationEvent) {
        // Ensure this event was addressed to me
        if (event.to == myPhone.value) {
            val senderLabel = partnerNickname.value
            val currentStatus = StatusType.fromKey(myStatus.value)

            if (currentStatus == StatusType.FREE) {
                // Trigger physical vibration
                triggerDeviceVibrator(450)
                _vibrationFeedbackActive.value = true
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _vibrationFeedbackActive.value = false
                }
                
                _lastPingStatus.value = "Success: Ping Triggered!"
                addLog("🔔 মনের সংযোগ! \"$senderLabel\" এর অনুভূতিতে আপনার ফোন কেঁপে উঠেছে (৪৫০ মিলি সেকেন্ড)।")
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
            .apply()

        // Notify Widget Broascast Receiver
        val intent = Intent(context, com.example.surjomukhi.widget.SurjomukhiWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, com.example.surjomukhi.widget.SurjomukhiWidgetProvider::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.shutdown()
    }
}
