package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AndroidNetworkMonitor
import com.example.ui.DinoyoAppScreen
import com.example.ui.DinoyoViewModel
import com.example.ui.theme.DinoyoTheme

class MainActivity : ComponentActivity() {
  private val viewModel: DinoyoViewModel by viewModels {
    DinoyoViewModel.Factory(AndroidNetworkMonitor(applicationContext))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
          onExitApp = { finish() },
        )
      }
    }
  }
}
