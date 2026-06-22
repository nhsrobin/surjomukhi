package com.example.surjomukhi

import androidx.compose.ui.graphics.Color

enum class StatusType(
    val key: String,
    val labelEn: String,
    val labelBn: String,
    val subtextEn: String,
    val subtextBn: String,
    val color: Color,
    val glowColor: Color
) {
    FREE(
        key = "free",
        labelEn = "Free",
        labelBn = "ফ্রি 🟢",
        subtextEn = "Available to chat or call",
        subtextBn = "ফ্রি আছি, আড্ডা দেওয়া যাবে",
        color = Color(0xFF00FF88), // Energized neon green
        glowColor = Color(0x3300FF88)
    ),
    STUDYING(
        key = "studying",
        labelEn = "Studying",
        labelBn = "পড়ছি 📚",
        subtextEn = "Focused & absorbed in work",
        subtextBn = "পড়াশোনা বা গুরুত্বপূর্ণ কাজে মনোযোগ দিচ্ছি",
        color = Color(0xFFFFD700), // Brilliant yellow
        glowColor = Color(0x33FFD700)
    ),
    SLEEPING(
        key = "sleeping",
        labelEn = "Sleeping",
        labelBn = "ঘুমাচ্ছি 💤",
        subtextEn = "Do not disturb, sleeping",
        subtextBn = "ঘুমের মাঝে আছি, বিরক্ত করবেন না",
        color = Color(0xFF3B82F6), // Calm sky blue
        glowColor = Color(0x333B82F6)
    ),
    SCROLLING(
        key = "scrolling",
        labelEn = "Scrolling",
        labelBn = "স্ক্রলিং 📱",
        subtextEn = "Just surfing or scrolling feed",
        subtextBn = "এমনিই সামাজিক মাধ্যমে স্ক্রল করছি",
        color = Color(0xFFEC4899), // Hot pink
        glowColor = Color(0x33EC4899)
    ),
    TALKING(
        key = "talking",
        labelEn = "On Call",
        labelBn = "কথা বলছি 📞",
        subtextEn = "Currently talking or on a call",
        subtextBn = "অন্য কারও সাথে বা কলে ফোনে কথা বলছি",
        color = Color(0xFF14B8A6), // Cyan teal
        glowColor = Color(0x3314B8A6)
    ),
    HOME(
        key = "home",
        labelEn = "At Home",
        labelBn = "বাসায় 🏠",
        subtextEn = "Safe at home, taking rest",
        subtextBn = "বাসায় নিরাপদে অবসরে বিশ্রাম নিচ্ছি",
        color = Color(0xFFF59E0B), // Vibrant amber
        glowColor = Color(0x33F59E0B)
    ),
    OFFLINE(
        key = "offline",
        labelEn = "Offline",
        labelBn = "নেটের বাইরে 📡",
        subtextEn = "Poor network or offline",
        subtextBn = "ইন্টারনেট বা নেটের বাইরে বিচ্ছিন্ন আছি",
        color = Color(0xFF9CA3AF), // Steel gray
        glowColor = Color(0x339CA3AF)
    ),
    BUSY(
        key = "busy",
        labelEn = "Busy",
        labelBn = "ব্যস্ত 🔴",
        subtextEn = "Extremely occupied, DND",
        subtextBn = "অন্য দরকারি কাজে ব্যস্ত আছি, পরে কথা হবে",
        color = Color(0xFFEF4444), // Crimson alarm red
        glowColor = Color(0x33EF4444)
    ),
    CUSTOM(
        key = "custom",
        labelEn = "Custom",
        labelBn = "কাস্টম 🔮",
        subtextEn = "Your personalized active status",
        subtextBn = "আপনার নিজের মতো করে স্টেটাস সেট করুন",
        color = Color(0xFFA855F7), // Neon purple
        glowColor = Color(0x33A855F7)
    );

    companion object {
        fun fromKey(key: String?): StatusType {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: FREE
        }
    }
}
