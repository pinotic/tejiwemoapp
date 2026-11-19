package com.example.ui

object DinoyoConfig {
  const val HOME_URL = "https://dinoyo.vercel.app/"
  const val HOME_HOST = "dinoyo.vercel.app"
}

sealed interface WebContentState {
  data object Ready : WebContentState

  data class Error(
    val errorCode: Int,
    val description: String,
    val isOffline: Boolean,
  ) : WebContentState
}

data class DinoyoUiState(
  val currentUrl: String = DinoyoConfig.HOME_URL,
  val pageTitle: String = "RT 1/ RW 5 Dinoyo - Malang",
  val loadProgress: Int = 0,
  val isLoading: Boolean = true,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val isOnline: Boolean = true,
  val hasLoadedOnce: Boolean = false,
  val contentState: WebContentState = WebContentState.Ready,
)

sealed interface WebViewCommand {
  data object Reload : WebViewCommand
  data object GoBack : WebViewCommand
  data class LoadUrl(val url: String) : WebViewCommand
}
