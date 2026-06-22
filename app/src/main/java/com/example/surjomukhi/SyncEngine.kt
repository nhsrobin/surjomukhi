package com.example.surjomukhi

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
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

    // Direct event channel for vibrations
    private val _vibrationEvents = MutableSharedFlow<VibrationEvent>(0, 64)
    val vibrationEvents: SharedFlow<VibrationEvent> = _vibrationEvents

    // Firestore Listener Registrations
    private var myDocListener: ListenerRegistration? = null
    private var partnerDocListener: ListenerRegistration? = null
    private var vibrationListener: ListenerRegistration? = null

    init {
        // Load persistent local configurations
        loadLocalPreferences()
        initializeFirebase()
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
            .apply()

        // Sync with Widget immediately
        try {
            val intent = Intent(context, com.example.surjomukhi.widget.SurjomukhiWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, com.example.surjomukhi.widget.SurjomukhiWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast widget update from SyncEngine", e)
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
                "updatedAt" to System.currentTimeMillis()
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
            // 1. Listen to MY document (to detect if partner binds to me, or modifies my nickname)
            myDocListener = db?.collection("users")?.document(phone)
                ?.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen self failed", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val boundPhone = snapshot.getString("boundPartnerPhone") ?: ""
                        val selfNickname = snapshot.getString("nickname") ?: "আমার মন"
                        
                        myNickname.value = selfNickname
                        
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
                            partnerUpdatedAt.value = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                            saveLocalPreferences()
                        }
                    }
            }

            // 3. Listen to incoming vibration pings directed to me
            vibrationListener = db?.collection("vibrations")
                ?.whereEqualTo("toPhone", phone)
                ?.whereGreaterThan("timestamp", System.currentTimeMillis() - 30_000)
                ?.addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen vibration events failed", error)
                        return@addSnapshotListener
                    }
                    if (querySnapshot != null) {
                        for (doc in querySnapshot.documentChanges) {
                            val changeDoc = doc.document
                            val timestamp = changeDoc.getLong("timestamp") ?: 0
                            val fromPhone = changeDoc.getString("fromPhone") ?: ""
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

    fun sendVibrationPing() {
        val from = myPhone.value
        val to = partnerPhone.value
        val timestamp = System.currentTimeMillis()

        if (from.isEmpty() || to.isEmpty()) return

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
        shutdownListeners()
    }
}
