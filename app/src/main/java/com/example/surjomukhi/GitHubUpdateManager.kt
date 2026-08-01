package com.example.surjomukhi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseTitle: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val publishedAt: String
)

class GitHubUpdateManager(private val context: Context) {
    private val TAG = "GitHubUpdateManager"
    private val prefs = context.getSharedPreferences("surjomukhi_github_prefs", Context.MODE_PRIVATE)

    // Current GitHub repository owner/name (nhsrobin/surjomukhi)
    val githubRepo = MutableStateFlow(
        prefs.getString("github_repo_slug", "nhsrobin/surjomukhi") ?: "nhsrobin/surjomukhi"
    )

    // Update Status States
    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _latestRelease = MutableStateFlow<GitHubReleaseInfo?>(null)
    val latestRelease: StateFlow<GitHubReleaseInfo?> = _latestRelease.asStateFlow()

    private val _lastCheckedTime = MutableStateFlow(
        prefs.getLong("last_github_check_time", 0L)
    )
    val lastCheckedTime: StateFlow<Long> = _lastCheckedTime.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateRepoSlug(newSlug: String) {
        val cleanSlug = newSlug.trim().removePrefix("https://github.com/").removeSuffix(".git").removeSuffix("/")
        if (cleanSlug.isNotBlank()) {
            prefs.edit().putString("github_repo_slug", cleanSlug).apply()
            githubRepo.value = cleanSlug
        }
    }

    suspend fun checkForUpdates(currentVersionName: String = "1.0.0"): Boolean = withContext(Dispatchers.IO) {
        _isChecking.value = true
        _statusMessage.value = "অ্যাপের সর্বশেষ সংস্করণ পরীক্ষা করা হচ্ছে..."
        
        val slug = githubRepo.value
        if (slug.isBlank() || !slug.contains("/")) {
            _statusMessage.value = "আপডেট কনফিগারেশন চেক করুন"
            _isChecking.value = false
            return@withContext false
        }

        try {
            // 1. Try fetching latest Release from GitHub API
            val releaseUrl = "https://api.github.com/repos/$slug/releases/latest"
            val responseJson = makeHttpGetRequest(releaseUrl)

            if (responseJson != null && responseJson.startsWith("{")) {
                val jsonObject = JSONObject(responseJson)
                val tagName = jsonObject.optString("tag_name", "v1.0.0")
                val releaseName = jsonObject.optString("name", "সূর্যমুখী নতুন আপডেট")
                val body = jsonObject.optString("body", "সূর্যমুখী অ্যাপে নতুন বৈশিষ্ট্য ও পারফরম্যান্স আপডেট যুক্ত করা হয়েছে।")
                val htmlUrl = jsonObject.optString("html_url", "https://github.com/$slug")
                val publishedAt = jsonObject.optString("published_at", "")

                // Find direct APK download url in release assets if available
                var apkUrl: String? = null
                if (jsonObject.has("assets")) {
                    val assets = jsonObject.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                val release = GitHubReleaseInfo(
                    tagName = tagName,
                    releaseTitle = releaseName,
                    body = body,
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = apkUrl ?: htmlUrl,
                    publishedAt = publishedAt
                )

                _latestRelease.value = release
                _lastCheckedTime.value = System.currentTimeMillis()
                prefs.edit().putLong("last_github_check_time", System.currentTimeMillis()).apply()

                // Version comparison
                val isNewer = isVersionNewer(tagName, currentVersionName)
                _isUpdateAvailable.value = isNewer

                if (isNewer) {
                    _statusMessage.value = "🎉 সূর্যমুখী অ্যাপের নতুন ভার্সন ($tagName) এসেছে!"
                    showSystemNotification(
                        title = "🎉 সূর্যমুখী অ্যাপের নতুন ভার্সন এসেছে!",
                        message = "নতুন ভার্সন $tagName আপডেট উপলব্ধ। আপনার অ্যাপটি আপডেট করতে ট্যাপ করুন।",
                        apkUrl = release.apkDownloadUrl ?: release.htmlUrl
                    )
                } else {
                    _statusMessage.value = "আপনার সূর্যমুখী অ্যাপটি সর্বশেষ ভার্সনে আপডেট রয়েছে।"
                }

                _isChecking.value = false
                return@withContext isNewer
            } else {
                // 2. Fallback: Check latest commits if no formal Release tag exists yet
                val commitsUrl = "https://api.github.com/repos/$slug/commits/main"
                val commitResponse = makeHttpGetRequest(commitsUrl)
                if (commitResponse != null && commitResponse.startsWith("{")) {
                    val commitJson = JSONObject(commitResponse)
                    val sha = commitJson.optString("sha", "").take(7)
                    val commitMsg = commitJson.optJSONObject("commit")?.optString("message") ?: "নতুন অ্যাপ ফিচার আপডেট"
                    val htmlUrl = commitJson.optString("html_url", "https://github.com/$slug")

                    val release = GitHubReleaseInfo(
                        tagName = "v1.0.$sha",
                        releaseTitle = "সূর্যমুখী নতুন সংস্করণ",
                        body = commitMsg,
                        htmlUrl = htmlUrl,
                        apkDownloadUrl = htmlUrl,
                        publishedAt = ""
                    )

                    val savedSha = prefs.getString("last_github_sha", "") ?: ""
                    val isNewCommit = savedSha.isNotEmpty() && savedSha != sha

                    _latestRelease.value = release
                    _isUpdateAvailable.value = isNewCommit
                    _statusMessage.value = if (isNewCommit) "🎉 সূর্যমুখী অ্যাপে নতুন আপডেট পাওয়া গেছে!" else "আপনার অ্যাপটি লেটেস্ট সংস্করণে আপডেট করা আছে।"

                    if (isNewCommit) {
                        showSystemNotification(
                            title = "🎉 সূর্যমুখী অ্যাপের নতুন আপডেট পাওয়া গেছে!",
                            message = "নতুন বৈশিষ্ট্য যুক্ত হয়েছে। অ্যাপটি আপডেট করতে এখানে ট্যাপ করুন।",
                            apkUrl = release.apkDownloadUrl ?: release.htmlUrl
                        )
                    }

                    _isChecking.value = false
                    return@withContext isNewCommit
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking updates: ${e.message}", e)
            _statusMessage.value = "সর্বশেষ সংস্করণ চেক করতে সমস্যা হয়েছে, ইন্টারনেট সংযোগ পরীক্ষা করুন।"
        }

        _isChecking.value = false
        return@withContext false
    }

    fun dismissUpdate() {
        _isUpdateAvailable.value = false
    }

    private fun showSystemNotification(title: String, message: String, apkUrl: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channelId = "surjomukhi_app_updates"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "সূর্যমুখী অ্যাপ আপডেট",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "সূর্যমুখী অ্যাপের নতুন ভার্সন আপডেট নোটিফিকেশন"
                    enableLights(true)
                    lightColor = android.graphics.Color.GREEN
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post system update notification: ${e.message}")
        }
    }

    private fun isVersionNewer(remoteVersionTag: String, localVersionName: String): Boolean {
        val remoteClean = remoteVersionTag.replace(Regex("[^0-9.]"), "")
        val localClean = localVersionName.replace(Regex("[^0-9.]"), "")

        if (remoteClean.isBlank()) return false
        if (localClean.isBlank()) return true

        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = localClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun makeHttpGetRequest(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Surjomukhi-Android-App")
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP Request Error: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }
}

