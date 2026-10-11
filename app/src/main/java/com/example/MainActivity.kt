package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.surjomukhi.MainViewModel
import com.example.surjomukhi.SurjomukhiDashboard
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
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
        ) {
          // Main Dashboard UI (or Auth screen if not logged in)
          SurjomukhiDashboard(viewModel = viewModel)
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

