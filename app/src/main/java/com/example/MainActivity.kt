package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AndroidNetworkMonitor
import com.example.fcm.DinoyoNotificationHelper
import com.example.ui.DinoyoAppScreen
import com.example.ui.DinoyoConfig
import com.example.ui.DinoyoViewModel
import com.example.ui.theme.DinoyoTheme

class MainActivity : ComponentActivity() {
  private val viewModel: DinoyoViewModel by viewModels {
    DinoyoViewModel.Factory(AndroidNetworkMonitor(applicationContext))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DinoyoNotificationHelper.createNotificationChannel(this)

    val launchUrl = extractTargetUrl(intent) ?: DinoyoConfig.HOME_URL

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT,
      ),
      navigationBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT,
      ),
    )
    setContent {
      DinoyoTheme(darkTheme = false, dynamicColor = false) {
        DinoyoAppScreen(
          viewModel = viewModel,
          initialUrl = launchUrl,
          onExitApp = { finish() },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    extractTargetUrl(intent)?.let { targetUrl ->
      viewModel.openNotificationUrl(targetUrl)
    }
  }

  private fun extractTargetUrl(intent: Intent?): String? {
    if (intent == null) return null
    val customUrl = intent.getStringExtra(DinoyoNotificationHelper.EXTRA_TARGET_URL)
    if (!customUrl.isNullOrBlank()) return customUrl
    val fcmDataUrl = intent.getStringExtra("url")
    return if (!fcmDataUrl.isNullOrBlank()) {
      DinoyoNotificationHelper.resolveTargetUrl(fcmDataUrl)
    } else {
      null
    }
  }
}
