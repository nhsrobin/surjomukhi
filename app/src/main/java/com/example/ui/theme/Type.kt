package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

import androidx.compose.ui.text.font.FontLoadingStrategy

val HindSiliguriFontFamily = FontFamily(
    Font(resId = R.font.hind_siliguri_light, weight = FontWeight.Light, loadingStrategy = FontLoadingStrategy.Async),
    Font(resId = R.font.hind_siliguri_regular, weight = FontWeight.Normal, loadingStrategy = FontLoadingStrategy.Async),
    Font(resId = R.font.hind_siliguri_medium, weight = FontWeight.Medium, loadingStrategy = FontLoadingStrategy.Async),
    Font(resId = R.font.hind_siliguri_semibold, weight = FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.Async),
    Font(resId = R.font.hind_siliguri_bold, weight = FontWeight.Bold, loadingStrategy = FontLoadingStrategy.Async)
)

// Set of Material typography styles mapping Hind Siliguri as primary font
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
)
