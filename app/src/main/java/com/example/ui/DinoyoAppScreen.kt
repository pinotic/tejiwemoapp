package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.webview.DinoyoWebView
import com.example.ui.webview.openInExternalBrowser
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

  BackHandler(enabled = uiState.isMoreSheetOpen) {
    viewModel.setMoreSheetOpen(false)
  }

  BackHandler(enabled = !uiState.isMoreSheetOpen && uiState.canGoBack) {
    viewModel.goBack()
  }

  BackHandler(enabled = !uiState.isMoreSheetOpen && !uiState.canGoBack) {
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

  val shareCurrentPage: () -> Unit = {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, uiState.pageTitle)
      putExtra(Intent.EXTRA_TEXT, uiState.currentUrl)
    }
    context.startActivity(
      Intent.createChooser(shareIntent, context.getString(R.string.msg_share_chooser))
    )
  }

  val copyCurrentLink: () -> Unit = {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Dinoyo URL", uiState.currentUrl))
    viewModel.setMoreSheetOpen(false)
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(context.getString(R.string.msg_link_copied))
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWideLayout = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.then(
            if (uiState.isImmersiveMode || isWideLayout) Modifier.navigationBarsPadding()
            else Modifier
          ),
        )
      },
      bottomBar = {
        if (!isWideLayout) {
          AnimatedVisibility(
            visible = !uiState.isImmersiveMode,
            enter = slideInVertically(animationSpec = spring()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = spring()) { it } + fadeOut(),
          ) {
            DinoyoBottomBar(
              canGoBack = uiState.canGoBack,
              isLoading = uiState.isLoading,
              isAtHome = uiState.currentUrl.trimEnd('/') == DinoyoConfig.HOME_URL.trimEnd('/'),
              onBack = viewModel::goBack,
              onHome = viewModel::navigateHome,
              onReload = viewModel::reload,
              onShare = shareCurrentPage,
              onMore = { viewModel.setMoreSheetOpen(true) },
            )
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (isWideLayout && !uiState.isImmersiveMode) {
          DinoyoNavigationRail(
            canGoBack = uiState.canGoBack,
            canGoForward = uiState.canGoForward,
            isAtHome = uiState.currentUrl.trimEnd('/') == DinoyoConfig.HOME_URL.trimEnd('/'),
            onHome = viewModel::navigateHome,
            onBack = viewModel::goBack,
            onForward = viewModel::goForward,
            onReload = viewModel::reload,
            onShare = shareCurrentPage,
            onMore = { viewModel.setMoreSheetOpen(true) },
          )
        }

        DinoyoWebViewport(
          uiState = uiState,
          commands = viewModel.commands,
          isWideLayout = isWideLayout,
          onProgressChanged = viewModel::onProgressChanged,
          onPageStarted = viewModel::onPageStarted,
          onPageFinished = viewModel::onPageFinished,
          onHistoryUpdated = viewModel::onHistoryUpdated,
          onTitleReceived = viewModel::onTitleReceived,
          onMainFrameError = viewModel::onMainFrameError,
          onRetry = viewModel::retryLoad,
          onGoBack = viewModel::goBack,
          onToggleImmersive = viewModel::toggleImmersiveMode,
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
            .weight(1f)
            .fillMaxHeight(),
        )
      }
    }

    if (uiState.isMoreSheetOpen) {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = { viewModel.setMoreSheetOpen(false) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
      ) {
        DinoyoMoreSheetContent(
          pageTitle = uiState.pageTitle,
          currentUrl = uiState.currentUrl,
          isDesktopMode = uiState.isDesktopMode,
          isImmersiveMode = uiState.isImmersiveMode,
          onToggleDesktopMode = viewModel::toggleDesktopMode,
          onToggleImmersiveMode = viewModel::toggleImmersiveMode,
          onOpenInBrowser = {
            viewModel.setMoreSheetOpen(false)
            openInExternalBrowser(context, uiState.currentUrl) {
              scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_no_external_app))
              }
            }
          },
          onCopyLink = copyCurrentLink,
          onClearCache = {
            viewModel.clearCacheAndReload()
            scope.launch {
              snackbarHostState.showSnackbar(context.getString(R.string.msg_cache_cleared))
            }
          },
        )
      }
    }
  }
}

@Composable
private fun DinoyoWebViewport(
  uiState: DinoyoUiState,
  commands: SharedFlow<WebViewCommand>,
  isWideLayout: Boolean,
  onProgressChanged: (Int) -> Unit,
  onPageStarted: (String) -> Unit,
  onPageFinished: (String, String?, Boolean, Boolean) -> Unit,
  onHistoryUpdated: (String, Boolean, Boolean) -> Unit,
  onTitleReceived: (String?) -> Unit,
  onMainFrameError: (Int, String) -> Unit,
  onRetry: () -> Unit,
  onGoBack: () -> Unit,
  onToggleImmersive: () -> Unit,
  onOpenInBrowser: () -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .then(
        if (uiState.isImmersiveMode || isWideLayout) Modifier.navigationBarsPadding()
        else Modifier
      ),
  ) {
    DinoyoWebView(
      initialUrl = DinoyoConfig.HOME_URL,
      isDesktopMode = uiState.isDesktopMode,
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

    AnimatedVisibility(
      visible = uiState.isImmersiveMode,
      enter = fadeIn() + scaleIn(initialScale = 0.85f),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("restore_controls_button"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
          if (uiState.canGoBack) {
            FilledTonalIconButton(
              onClick = onGoBack,
              modifier = Modifier
                .size(48.dp)
                .testTag("immersive_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
              )
            }
          }
          FilledTonalIconButton(
            onClick = onToggleImmersive,
            modifier = Modifier
              .size(48.dp)
              .testTag("immersive_exit_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.FullscreenExit,
              contentDescription = stringResource(R.string.show_controls_label),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DinoyoBottomBar(
  canGoBack: Boolean,
  isLoading: Boolean,
  isAtHome: Boolean,
  onBack: () -> Unit,
  onHome: () -> Unit,
  onReload: () -> Unit,
  onShare: () -> Unit,
  onMore: () -> Unit,
) {
  NavigationBar(
    windowInsets = WindowInsets.navigationBars,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 2.dp,
    modifier = Modifier.testTag("dinoyo_bottom_bar"),
  ) {
    NavigationBarItem(
      selected = false,
      onClick = onBack,
      enabled = canGoBack,
      icon = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.nav_back),
          modifier = Modifier.alpha(if (canGoBack) 1f else 0.38f),
        )
      },
      label = {
        Text(
          text = stringResource(R.string.nav_back),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      modifier = Modifier.testTag("nav_back_button"),
    )

    NavigationBarItem(
      selected = isAtHome,
      onClick = onHome,
      icon = {
        Icon(
          imageVector = Icons.Filled.Home,
          contentDescription = stringResource(R.string.nav_home),
        )
      },
      label = {
        Text(
          text = stringResource(R.string.nav_home),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      modifier = Modifier.testTag("nav_home_button"),
    )

    NavigationBarItem(
      selected = false,
      onClick = onReload,
      icon = {
        Icon(
          imageVector = if (isLoading) Icons.Outlined.Refresh else Icons.Filled.Refresh,
          contentDescription = stringResource(R.string.nav_reload),
        )
      },
      label = {
        Text(
          text = stringResource(R.string.nav_reload),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      modifier = Modifier.testTag("nav_reload_button"),
    )

    NavigationBarItem(
      selected = false,
      onClick = onShare,
      icon = {
        Icon(
          imageVector = Icons.Filled.Share,
          contentDescription = stringResource(R.string.nav_share),
        )
      },
      label = {
        Text(
          text = stringResource(R.string.nav_share),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      modifier = Modifier.testTag("nav_share_button"),
    )

    NavigationBarItem(
      selected = false,
      onClick = onMore,
      icon = {
        Icon(
          imageVector = Icons.Filled.MoreHoriz,
          contentDescription = stringResource(R.string.nav_more),
        )
      },
      label = {
        Text(
          text = stringResource(R.string.nav_more),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      modifier = Modifier.testTag("nav_more_button"),
    )
  }
}

@Composable
private fun DinoyoNavigationRail(
  canGoBack: Boolean,
  canGoForward: Boolean,
  isAtHome: Boolean,
  onHome: () -> Unit,
  onBack: () -> Unit,
  onForward: () -> Unit,
  onReload: () -> Unit,
  onShare: () -> Unit,
  onMore: () -> Unit,
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    windowInsets = WindowInsets.safeDrawing.only(
      WindowInsetsSides.Start + WindowInsetsSides.Vertical
    ),
    header = {
      Image(
        painter = painterResource(id = R.drawable.img_dinoyo_icon_foreground),
        contentDescription = stringResource(R.string.cd_brand_logo),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .padding(top = 8.dp, bottom = 12.dp)
          .size(44.dp)
          .clip(RoundedCornerShape(14.dp)),
      )
    },
    modifier = Modifier.testTag("dinoyo_navigation_rail"),
  ) {
    NavigationRailItem(
      selected = isAtHome,
      onClick = onHome,
      icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_home)) },
      label = { Text(stringResource(R.string.nav_home)) },
      modifier = Modifier.testTag("rail_home_button"),
    )
    NavigationRailItem(
      selected = false,
      onClick = onBack,
      enabled = canGoBack,
      icon = {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.nav_back),
          modifier = Modifier.alpha(if (canGoBack) 1f else 0.38f),
        )
      },
      label = { Text(stringResource(R.string.nav_back)) },
      modifier = Modifier.testTag("rail_back_button"),
    )
    NavigationRailItem(
      selected = false,
      onClick = onForward,
      enabled = canGoForward,
      icon = {
        Icon(
          Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = stringResource(R.string.nav_forward),
          modifier = Modifier.alpha(if (canGoForward) 1f else 0.38f),
        )
      },
      label = { Text(stringResource(R.string.nav_forward)) },
      modifier = Modifier.testTag("rail_forward_button"),
    )
    NavigationRailItem(
      selected = false,
      onClick = onReload,
      icon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.nav_reload)) },
      label = { Text(stringResource(R.string.nav_reload)) },
      modifier = Modifier.testTag("rail_reload_button"),
    )
    Spacer(modifier = Modifier.weight(1f))
    NavigationRailItem(
      selected = false,
      onClick = onShare,
      icon = { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.nav_share)) },
      label = { Text(stringResource(R.string.nav_share)) },
      modifier = Modifier.testTag("rail_share_button"),
    )
    NavigationRailItem(
      selected = false,
      onClick = onMore,
      icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.nav_more)) },
      label = { Text(stringResource(R.string.nav_more)) },
      modifier = Modifier.testTag("rail_more_button"),
    )
  }
}
