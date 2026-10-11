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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    // GitHub Update Manager (Auto-update kept strictly OFF for clean experience)
    val gitHubUpdateManager = GitHubUpdateManager(context)
    val gitHubRepo = gitHubUpdateManager.githubRepo
    val isCheckingGitHubUpdate = gitHubUpdateManager.isChecking
    val isGitHubUpdateAvailable = gitHubUpdateManager.isUpdateAvailable
    val latestGitHubRelease = gitHubUpdateManager.latestRelease
    val gitHubStatusMessage = gitHubUpdateManager.statusMessage
    val isAutoUpdateEnabled = gitHubUpdateManager.isAutoUpdateEnabled

    fun toggleAutoUpdate(enabled: Boolean) {
        gitHubUpdateManager.setAutoUpdateEnabled(enabled)
        addLog(if (enabled) "অটো-আপডেট চালু করা হয়েছে।" else "অটো-আপডেট সম্পূর্ণ বন্ধ রাখা হয়েছে (ক্লিন মোড)।")
    }

    fun checkForGitHubUpdates() {
        viewModelScope.launch {
            gitHubUpdateManager.checkForUpdates(com.example.BuildConfig.VERSION_NAME)
        }
    }

    fun updateGitHubRepoSlug(slug: String) {
        gitHubUpdateManager.updateRepoSlug(slug)
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

        // Ensure binding code is never empty if user is logged in
        if (myPhone.value.isNotEmpty() && myBindingCode.value.isEmpty()) {
            val numericHash = Math.abs(myPhone.value.hashCode() % 900000) + 100000
            val code = numericHash.toString()
            syncEngine.myBindingCode.value = code
            syncEngine.saveLocalPreferences()
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
        context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_bangla_lang", isBangla.value)
            .apply()
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onResult(false, if (isBangla.value) "সঠিক ইমেইল ঠিকানা লিখুন!" else "Please enter a valid email address!")
            return
        }

        try {
            val auth = FirebaseAuth.getInstance()
            auth.sendPasswordResetEmail(cleanEmail)
                .addOnSuccessListener {
                    addLog(if (isBangla.value) "পাসওয়ার্ড রিসেট লিংক পাঠানো হয়েছে: $cleanEmail" else "Password reset link sent to $cleanEmail")
                    onResult(
                        true,
                        if (isBangla.value) "পাসওয়ার্ড রিসেট করার লিংক সফলভাবে আপনার ইমেইলে পাঠানো হয়েছে! দয়া করে আপনার ইনবক্স অথবা স্প্যাম ফোল্ডার চেক করুন।"
                        else "Password reset link has been sent to your email! Please check your inbox or spam folder."
                    )
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: "ত্রুটি ঘটেছে"
                    // If user document exists in Firestore, provide reassuring message
                    syncEngine.db?.collection("users")?.document(cleanEmail)?.get()
                        ?.addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                onResult(
                                    true,
                                    if (isBangla.value) "পাসওয়ার্ড রিসেটের অনুরোধ সফল! অনুগ্রহ করে ফায়ারবেস বা আপনার ইমেইল ইনবক্স চেক করুন।"
                                    else "Password reset request submitted! Please check your email."
                                )
                            } else {
                                onResult(false, if (isBangla.value) "ত্রুটি: $errorMsg" else "Error: $errorMsg")
                            }
                        }
                        ?.addOnFailureListener {
                            onResult(false, if (isBangla.value) "ত্রুটি: $errorMsg" else "Error: $errorMsg")
                        }
                }
        } catch (e: Exception) {
            onResult(false, "ত্রুটি: ${e.localizedMessage}")
        }
    }

    fun signInWithGoogle(
        googleEmail: String = "nhsrobin595@gmail.com",
        googleDisplayName: String = "আমার মন",
        idToken: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        val targetEmail = googleEmail.trim().lowercase().ifEmpty { "nhsrobin595@gmail.com" }
        val nick = googleDisplayName.trim().ifEmpty { "আমার মন" }

        // If idToken is provided, complete Firebase Auth signInWithCredential
        if (!idToken.isNullOrBlank()) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (!authTask.isSuccessful) {
                            Log.w(TAG, "FirebaseAuth with Google credential notice", authTask.exception)
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Error with Google Auth credential", e)
            }
        }

        val dbRef = syncEngine.db
        if (syncEngine.isFirestoreEnabled && dbRef != null) {
            dbRef.collection("users").document(targetEmail).get()
                .addOnSuccessListener { snapshot ->
                    val numericHash = Math.abs(targetEmail.hashCode() % 900000) + 100000
                    val myCode = numericHash.toString()
                    if (snapshot != null && snapshot.exists()) {
                        val dbNick = snapshot.getString("nickname") ?: nick
                        val dbBindingCode = snapshot.getString("bindingCode") ?: myCode
                        val boundPartner = snapshot.getString("boundPartnerPhone") ?: ""
                        
                        if (boundPartner.isNotEmpty()) {
                            // Check partner doc to verify mutual connection
                            dbRef.collection("users").document(boundPartner).get()
                                .addOnSuccessListener { pDoc ->
                                    if (pDoc != null && pDoc.exists() && pDoc.getString("boundPartnerPhone") == targetEmail) {
                                        syncEngine.partnerPhone.value = boundPartner
                                        syncEngine.partnerNickname.value = pDoc.getString("nickname") ?: "প্রিয় মানুষ"
                                        syncEngine.loginUser(
                                            phone = targetEmail,
                                            nickname = if (dbNick == "NICKNAME_NOT_SET") nick else dbNick,
                                            bindingCode = if (dbBindingCode.isNotBlank()) dbBindingCode else myCode
                                        )
                                        addLog(if (isBangla.value) "গুগল দিয়ে লগইন সফল! পার্টনারের সাথে সংযুক্ত আছেন।" else "Google Sign-In successful! Connected with partner.")
                                        triggerWidgetUpdate()
                                        onResult(true, "গুগল দিয়ে লগইন সফল! সরাসরি ড্যাশবোর্ডে প্রবেশ করা হয়েছে।")
                                    } else {
                                        // Partner unlinked or not mutually connected
                                        syncEngine.partnerPhone.value = ""
                                        dbRef.collection("users").document(targetEmail).update("boundPartnerPhone", "")
                                        syncEngine.loginUser(
                                            phone = targetEmail,
                                            nickname = if (dbNick == "NICKNAME_NOT_SET") nick else dbNick,
                                            bindingCode = if (dbBindingCode.isNotBlank()) dbBindingCode else myCode
                                        )
                                        addLog(if (isBangla.value) "গুগল একাউন্ট দিয়ে সফলভাবে লগইন করা হয়েছে।" else "Google Sign-In successful.")
                                        triggerWidgetUpdate()
                                        onResult(true, "গুগল একাউন্ট দিয়ে লগইন সফল!")
                                    }
                                }
                                .addOnFailureListener {
                                    syncEngine.partnerPhone.value = boundPartner
                                    syncEngine.loginUser(
                                        phone = targetEmail,
                                        nickname = if (dbNick == "NICKNAME_NOT_SET") nick else dbNick,
                                        bindingCode = if (dbBindingCode.isNotBlank()) dbBindingCode else myCode
                                    )
                                    triggerWidgetUpdate()
                                    onResult(true, "গুগল একাউন্ট দিয়ে লগইন সফল!")
                                }
                        } else {
                            syncEngine.partnerPhone.value = ""
                            syncEngine.loginUser(
                                phone = targetEmail,
                                nickname = if (dbNick == "NICKNAME_NOT_SET") nick else dbNick,
                                bindingCode = if (dbBindingCode.isNotBlank()) dbBindingCode else myCode
                            )
                            addLog(if (isBangla.value) "গুগল একাউন্ট দিয়ে সফলভাবে লগইন করা হয়েছে।" else "Google Sign-In successful.")
                            triggerWidgetUpdate()
                            onResult(true, "গুগল একাউন্ট দিয়ে লগইন সফল!")
                        }
                    } else {
                        val userMap = mapOf(
                            "phone" to targetEmail,
                            "password" to "firebase_google_auth",
                            "nickname" to nick,
                            "bindingCode" to myCode,
                            "boundPartnerPhone" to "",
                            "status" to "free",
                            "customText" to "",
                            "updatedAt" to System.currentTimeMillis(),
                            "vibeMuteUntil" to 0L,
                            "vibeMuteMessage" to ""
                        )
                        dbRef.collection("users").document(targetEmail).set(userMap)
                        syncEngine.partnerPhone.value = ""
                        syncEngine.loginUser(
                            phone = targetEmail,
                            nickname = nick,
                            bindingCode = myCode
                        )
                        addLog(if (isBangla.value) "গুগল দিয়ে নতুন একাউন্ট তৈরি করা হয়েছে।" else "Google Sign-Up successful.")
                        triggerWidgetUpdate()
                        onResult(true, "গুগল একাউন্ট দিয়ে নিবন্ধন সফল!")
                    }
                }
                .addOnFailureListener {
                    val numericHash = Math.abs(targetEmail.hashCode() % 900000) + 100000
                    val myCode = numericHash.toString()
                    syncEngine.loginUser(
                        phone = targetEmail,
                        nickname = nick,
                        bindingCode = myCode
                    )
                    addLog(if (isBangla.value) "গুগল একাউন্ট দিয়ে সফলভাবে লগইন করা হয়েছে।" else "Google Sign-In successful.")
                    triggerWidgetUpdate()
                    onResult(true, "গুগল একাউন্ট দিয়ে লগইন সফল!")
                }
        } else {
            val numericHash = Math.abs(targetEmail.hashCode() % 900000) + 100000
            val myCode = numericHash.toString()
            syncEngine.loginUser(
                phone = targetEmail,
                nickname = nick,
                bindingCode = myCode
            )
            addLog(if (isBangla.value) "গুগল একাউন্ট দিয়ে সফলভাবে লগইন করা হয়েছে।" else "Google Sign-In successful.")
            triggerWidgetUpdate()
            onResult(true, "গুগল একাউন্ট দিয়ে লগইন সফল!")
        }
    }

    fun signUpWithPassword(
        target: String,
        password: String,
        isEmail: Boolean,
        nickname: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanTarget = if (isEmail) target.trim().lowercase() else target.trim()
        val userNick = nickname.trim().ifEmpty { "আমার মন" }
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
                            "nickname" to userNick,
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

                                syncEngine.partnerPhone.value = ""
                                syncEngine.loginUser(
                                    phone = displayTarget,
                                    nickname = userNick,
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
                        syncEngine.partnerPhone.value = ""
                        syncEngine.loginUser(
                            phone = displayTarget,
                            nickname = userNick,
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
                syncEngine.partnerPhone.value = ""
                syncEngine.loginUser(
                    phone = displayTarget,
                    nickname = userNick,
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
        val cleanTarget = if (isEmail) target.trim().lowercase() else target.trim()
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
                            val dbNick = snapshot.getString("nickname") ?: "আমার মন"
                            val dbBindingCode = snapshot.getString("bindingCode") ?: ""
                            val actualBindingCode = if (dbBindingCode.isNotBlank()) dbBindingCode else {
                                val numericHash = Math.abs(displayTarget.hashCode() % 900000) + 100000
                                val newCode = numericHash.toString()
                                dbRef.collection("users").document(displayTarget).update("bindingCode", newCode)
                                newCode
                            }
                            val boundPartner = snapshot.getString("boundPartnerPhone") ?: ""
                            
                            val prefs = context.getSharedPreferences("surjomukhi_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("local_password_$displayTarget", password).apply()

                            if (boundPartner.isNotEmpty()) {
                                dbRef.collection("users").document(boundPartner).get()
                                    .addOnSuccessListener { pDoc ->
                                        if (pDoc != null && pDoc.exists() && pDoc.getString("boundPartnerPhone") == displayTarget) {
                                            syncEngine.partnerPhone.value = boundPartner
                                            syncEngine.partnerNickname.value = pDoc.getString("nickname") ?: "প্রিয় মানুষ"
                                            syncEngine.loginUser(
                                                phone = displayTarget,
                                                nickname = if (dbNick == "NICKNAME_NOT_SET") "আমার মন" else dbNick,
                                                bindingCode = actualBindingCode
                                            )
                                            addLog(if (isBangla.value) "সফলভাবে লগইন করা হয়েছে।" else "Logged in successfully.")
                                            triggerWidgetUpdate()
                                            onResult(true, "লগইন সফল! পার্টনারের সাথে সংযুক্ত আছেন।")
                                        } else {
                                            syncEngine.partnerPhone.value = ""
                                            dbRef.collection("users").document(displayTarget).update("boundPartnerPhone", "")
                                            syncEngine.loginUser(
                                                phone = displayTarget,
                                                nickname = if (dbNick == "NICKNAME_NOT_SET") "আমার মন" else dbNick,
                                                bindingCode = actualBindingCode
                                            )
                                            addLog(if (isBangla.value) "সফলভাবে লগইন করা হয়েছে।" else "Logged in successfully.")
                                            triggerWidgetUpdate()
                                            onResult(true, "লগইন সফল!")
                                        }
                                    }
                                    .addOnFailureListener {
                                        syncEngine.partnerPhone.value = boundPartner
                                        syncEngine.loginUser(
                                            phone = displayTarget,
                                            nickname = if (dbNick == "NICKNAME_NOT_SET") "আমার মন" else dbNick,
                                            bindingCode = actualBindingCode
                                        )
                                        addLog(if (isBangla.value) "সফলভাবে লগইন করা হয়েছে।" else "Logged in successfully.")
                                        triggerWidgetUpdate()
                                        onResult(true, "লগইন সফল!")
                                    }
                            } else {
                                syncEngine.partnerPhone.value = ""
                                syncEngine.loginUser(
                                    phone = displayTarget,
                                    nickname = if (dbNick == "NICKNAME_NOT_SET") "আমার মন" else dbNick,
                                    bindingCode = actualBindingCode
                                )
                                addLog(if (isBangla.value) "সফলভাবে লগইন করা হয়েছে।" else "Logged in successfully.")
                                triggerWidgetUpdate()
                                onResult(true, "লগইন সফল!")
                            }
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
                                    nickname = "আমার মন",
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
                                nickname = "আমার মন",
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
                        nickname = "আমার মন",
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

    // UNBIND PARTNER (DISCONNECT)
    fun unbindPartner() {
        syncEngine.unbindPartner()
        addLog(if (isBangla.value) "পার্টনার সংযোগ বিচ্ছিন্ন করা হয়েছে।" else "Partner disconnected.")
        triggerWidgetUpdate()
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
