package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DinoyoViewModel(
  private val networkMonitor: NetworkConnectivityObserver,
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    DinoyoUiState(isOnline = networkMonitor.currentOnlineState())
  )
  val uiState: StateFlow<DinoyoUiState> = _uiState.asStateFlow()

  private val _commands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 8)
  val commands: SharedFlow<WebViewCommand> = _commands.asSharedFlow()

  init {
    viewModelScope.launch {
      networkMonitor.isOnline.collect { online ->
        val wasOfflineOnError = _uiState.value.contentState is WebContentState.Error &&
          (_uiState.value.contentState as WebContentState.Error).isOffline
        _uiState.update { state ->
          state.copy(isOnline = online)
        }
        if (online && wasOfflineOnError) {
          retryLoad()
        }
      }
    }
  }

  fun onProgressChanged(progress: Int) {
    _uiState.update { state ->
      state.copy(
        loadProgress = progress.coerceIn(0, 100),
        isLoading = progress < 100,
      )
    }
  }

  fun onPageStarted(url: String) {
    _uiState.update { state ->
      state.copy(
        currentUrl = url,
        isLoading = true,
        contentState = WebContentState.Ready,
      )
    }
  }

  fun onPageFinished(
    url: String,
    title: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
  ) {
    _uiState.update { state ->
      val cleanTitle = title?.takeIf { it.isNotBlank() && !it.startsWith("http") } ?: state.pageTitle
      state.copy(
        currentUrl = url.ifBlank { state.currentUrl },
        pageTitle = cleanTitle,
        loadProgress = 100,
        isLoading = false,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        hasLoadedOnce = state.hasLoadedOnce || state.contentState is WebContentState.Ready,
      )
    }
  }

  fun onHistoryUpdated(
    url: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
  ) {
    _uiState.update { state ->
      state.copy(
        currentUrl = url.ifBlank { state.currentUrl },
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onTitleReceived(title: String?) {
    if (!title.isNullOrBlank() && !title.startsWith("http")) {
      _uiState.update { it.copy(pageTitle = title) }
    }
  }

  fun onMainFrameError(errorCode: Int, description: String) {
    val offline = !networkMonitor.currentOnlineState()
    _uiState.update { state ->
      state.copy(
        isLoading = false,
        loadProgress = 0,
        isOnline = !offline,
        contentState = WebContentState.Error(
          errorCode = errorCode,
          description = description,
          isOffline = offline,
        ),
      )
    }
  }

  fun goBack() {
    _commands.tryEmit(WebViewCommand.GoBack)
  }

  fun openNotificationUrl(url: String) {
    if (url.isBlank()) return
    _uiState.update { state ->
      state.copy(
        currentUrl = url,
        contentState = WebContentState.Ready,
      )
    }
    _commands.tryEmit(WebViewCommand.LoadUrl(url))
  }

  fun retryLoad() {
    val currentOnline = networkMonitor.currentOnlineState()
    _uiState.update { state ->
      state.copy(
        isOnline = currentOnline,
        isLoading = true,
        contentState = WebContentState.Ready,
      )
    }
    _commands.tryEmit(WebViewCommand.Reload)
  }

  class Factory(
    private val networkMonitor: NetworkConnectivityObserver,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(DinoyoViewModel::class.java)) {
        return DinoyoViewModel(networkMonitor) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
