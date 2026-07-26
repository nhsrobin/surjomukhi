package com.example.surjomukhi

import android.content.Context
import android.util.Log
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

    // Current GitHub repository owner/name (e.g., "tasfiwnlabs/surjomukhi")
    val githubRepo = MutableStateFlow(
        prefs.getString("github_repo_slug", "tasfiwnlabs/surjomukhi") ?: "tasfiwnlabs/surjomukhi"
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
        prefs.edit().putString("github_repo_slug", cleanSlug).apply()
        githubRepo.value = cleanSlug
    }

    suspend fun checkForUpdates(currentVersionName: String = "1.0.0"): Boolean = withContext(Dispatchers.IO) {
        _isChecking.value = true
        _statusMessage.value = "গিটহাব থেকে সর্বশেষ পরিবর্তন চেক করা হচ্ছে..."
        
        val slug = githubRepo.value
        if (slug.isBlank() || !slug.contains("/")) {
            _statusMessage.value = "গিটহাব রেপো নাম সঠিক নয় (যেমন: username/repo)"
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
                val releaseName = jsonObject.optString("name", "New GitHub Release")
                val body = jsonObject.optString("body", "গিটহাবে নতুন কোড আপডেট করা হয়েছে।")
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
                    _statusMessage.value = "🚀 গিটহাবে নতুন আপডেট পাওয়া গেছে: $tagName"
                } else {
                    _statusMessage.value = "আপনার অ্যাপটি গিটহাবের সর্বশেষ কোডের সাথে আপ-টু-ডেট রয়েছে।"
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
                    val commitMsg = commitJson.optJSONObject("commit")?.optString("message") ?: "গিটহাব কমিট আপডেট"
                    val htmlUrl = commitJson.optString("html_url", "https://github.com/$slug")

                    val release = GitHubReleaseInfo(
                        tagName = "Commit $sha",
                        releaseTitle = "GitHub Latest Commit ($sha)",
                        body = commitMsg,
                        htmlUrl = htmlUrl,
                        apkDownloadUrl = htmlUrl,
                        publishedAt = ""
                    )

                    val savedSha = prefs.getString("last_github_sha", "") ?: ""
                    val isNewCommit = savedSha.isNotEmpty() && savedSha != sha

                    _latestRelease.value = release
                    _isUpdateAvailable.value = isNewCommit
                    _statusMessage.value = if (isNewCommit) "🚀 গিটহাবে নতুন কোড পরিবর্তন ($sha) পাওয়া গেছে!" else "গিটহাব কোড আপডেট চেক সম্পন্ন হয়েছে।"

                    _isChecking.value = false
                    return@withContext isNewCommit
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GitHub updates: ${e.message}", e)
            _statusMessage.value = "গিটহাব নেটওয়ার্ক কানেকশন ব্যর্থ হয়েছে: ${e.localizedMessage}"
        }

        _isChecking.value = false
        return@withContext false
    }

    fun dismissUpdate() {
        _isUpdateAvailable.value = false
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
