package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.surjomukhi.MainViewModel
import com.example.surjomukhi.SurjomukhiDashboard
import com.example.surjomukhi.SurjomukhiSplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Start background sync service as a standard service when app is in the foreground
    try {
      val prefs = getSharedPreferences("surjomukhi_prefs", android.content.Context.MODE_PRIVATE)
      val isBackgroundSyncEnabled = prefs.getBoolean("is_background_sync_enabled", true)
      if (isBackgroundSyncEnabled) {
        val serviceIntent = android.content.Intent(this, com.example.surjomukhi.SurjomukhiSyncService::class.java)
        startService(serviceIntent)
        
        // Start the silent background alarm scheduler to run indefinitely in the background
        com.example.surjomukhi.SurjomukhiAlarmReceiver.scheduleNextAlarm(this)
      }
    } catch (e: Exception) {
      android.util.Log.d("MainActivity", "Failed to start background sync service or alarm: ${e.message}")
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        var isSplashVisible by remember { mutableStateOf(true) }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
        ) {
          // Main Dashboard UI
          SurjomukhiDashboard(viewModel = viewModel)

          // Modern Animated Splash Screen (stays visible until app opens properly)
          AnimatedVisibility(
            visible = isSplashVisible,
            exit = fadeOut(animationSpec = tween(700))
          ) {
            SurjomukhiSplashScreen(
              onInitializationComplete = {
                isSplashVisible = false
              }
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Trigger any widget updates so the launcher reflects the latest state when returning
    viewModel.triggerWidgetUpdate()
  }
}

