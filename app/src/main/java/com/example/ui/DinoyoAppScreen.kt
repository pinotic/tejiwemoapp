package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.fcm.DinoyoNotificationHelper
import com.example.ui.webview.DinoyoWebView
import com.example.ui.webview.openInExternalBrowser
import kotlinx.coroutines.launch

private val DinoyoWebSurfaceColor = Color(0xFFF8F9FA)

@Composable
fun DinoyoAppScreen(
  viewModel: DinoyoViewModel,
  initialUrl: String,
  onExitApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var lastBackPressTime by remember { mutableLongStateOf(0L) }

  val showSyncError: (String) -> Unit = { errorMsg ->
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(errorMsg)
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      DinoyoNotificationHelper.syncFcmToken(context, onSyncError = showSyncError)
    }
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (granted) {
        DinoyoNotificationHelper.syncFcmToken(context, onSyncError = showSyncError)
      } else {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      DinoyoNotificationHelper.syncFcmToken(context, onSyncError = showSyncError)
    }
  }

  LaunchedEffect(uiState.isOnline) {
    if (uiState.isOnline) {
      val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
      if (hasPermission) {
        DinoyoNotificationHelper.syncFcmToken(context)
      }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
          ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
          ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
          DinoyoNotificationHelper.syncFcmToken(context)
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  BackHandler(enabled = uiState.canGoBack) {
    viewModel.goBack()
  }

  BackHandler(enabled = !uiState.canGoBack) {
    val now = System.currentTimeMillis()
    if (now - lastBackPressTime < 2000L) {
      onExitApp()
    } else {
      lastBackPressTime = now
      scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(context.getString(R.string.msg_back_exit_hint))
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = DinoyoWebSurfaceColor,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(DinoyoWebSurfaceColor)
        .padding(innerPadding)
        .statusBarsPadding()
        .navigationBarsPadding(),
    ) {
      if (uiState.isLoading && uiState.loadProgress in 1..99) {
        LinearProgressIndicator(
          progress = { uiState.loadProgress / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .testTag("webview_progress_bar"),
          color = MaterialTheme.colorScheme.primary,
          trackColor = DinoyoWebSurfaceColor,
        )
      }

      val errorState = uiState.contentState as? WebContentState.Error
      if (errorState != null) {
        DinoyoErrorScreen(
          isOffline = errorState.isOffline || !uiState.isOnline,
          onRetry = viewModel::retryLoad,
          onOpenInBrowser = {
            openInExternalBrowser(context, uiState.currentUrl) {
              scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_no_external_app))
              }
            }
          },
        )
      } else {
        DinoyoWebView(
          initialUrl = initialUrl,
          commands = viewModel.commands,
          onProgressChanged = viewModel::onProgressChanged,
          onPageStarted = viewModel::onPageStarted,
          onPageFinished = viewModel::onPageFinished,
          onHistoryUpdated = viewModel::onHistoryUpdated,
          onTitleReceived = viewModel::onTitleReceived,
          onMainFrameError = viewModel::onMainFrameError,
          onShowMessage = { message ->
            scope.launch {
              snackbarHostState.currentSnackbarData?.dismiss()
              snackbarHostState.showSnackbar(message)
            }
          },
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("dinoyo_webview"),
        )
      }
    }
  }
}
