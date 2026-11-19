package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.webview.DinoyoWebView
import com.example.ui.webview.openInExternalBrowser
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@Composable
fun DinoyoAppScreen(
  viewModel: DinoyoViewModel,
  onExitApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var lastBackPressTime by remember { mutableLongStateOf(0L) }

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
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  ) { innerPadding ->
    DinoyoWebViewport(
      uiState = uiState,
      commands = viewModel.commands,
      onProgressChanged = viewModel::onProgressChanged,
      onPageStarted = viewModel::onPageStarted,
      onPageFinished = viewModel::onPageFinished,
      onHistoryUpdated = viewModel::onHistoryUpdated,
      onTitleReceived = viewModel::onTitleReceived,
      onMainFrameError = viewModel::onMainFrameError,
      onRetry = viewModel::retryLoad,
      onOpenInBrowser = {
        openInExternalBrowser(context, uiState.currentUrl) {
          scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.msg_no_external_app))
          }
        }
      },
      onShowMessage = { message ->
        scope.launch {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(message)
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    )
  }
}

@Composable
private fun DinoyoWebViewport(
  uiState: DinoyoUiState,
  commands: SharedFlow<WebViewCommand>,
  onProgressChanged: (Int) -> Unit,
  onPageStarted: (String) -> Unit,
  onPageFinished: (String, String?, Boolean, Boolean) -> Unit,
  onHistoryUpdated: (String, Boolean, Boolean) -> Unit,
  onTitleReceived: (String?) -> Unit,
  onMainFrameError: (Int, String) -> Unit,
  onRetry: () -> Unit,
  onOpenInBrowser: () -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    DinoyoWebView(
      initialUrl = DinoyoConfig.HOME_URL,
      commands = commands,
      onProgressChanged = onProgressChanged,
      onPageStarted = onPageStarted,
      onPageFinished = onPageFinished,
      onHistoryUpdated = onHistoryUpdated,
      onTitleReceived = onTitleReceived,
      onMainFrameError = onMainFrameError,
      onShowMessage = onShowMessage,
      modifier = Modifier
        .fillMaxSize()
        .testTag("dinoyo_webview"),
    )

    AnimatedVisibility(
      visible = uiState.contentState is WebContentState.Error,
      enter = fadeIn(tween(220)),
      exit = fadeOut(tween(180)),
    ) {
      val errorState = uiState.contentState as? WebContentState.Error
      DinoyoErrorScreen(
        isOffline = errorState?.isOffline == true || !uiState.isOnline,
        onRetry = onRetry,
        onOpenInBrowser = onOpenInBrowser,
      )
    }

    AnimatedVisibility(
      visible = uiState.contentState is WebContentState.InitialLoading,
      enter = fadeIn(tween(150)),
      exit = fadeOut(tween(320)),
    ) {
      DinoyoInitialLoadingOverlay(progress = uiState.loadProgress)
    }

    AnimatedVisibility(
      visible = uiState.isLoading &&
        uiState.loadProgress in 1..99 &&
        uiState.contentState !is WebContentState.InitialLoading,
      enter = fadeIn(tween(120)),
      exit = fadeOut(tween(200)),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      LinearProgressIndicator(
        progress = { uiState.loadProgress / 100f },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .testTag("webview_progress_bar"),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      )
    }

    AnimatedVisibility(
      visible = !uiState.isOnline &&
        uiState.hasLoadedOnce &&
        uiState.contentState is WebContentState.Ready,
      enter = slideInVertically { -it } + fadeIn(),
      exit = slideOutVertically { -it } + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.testTag("offline_banner"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.offline_banner_text),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
    }
  }
}
