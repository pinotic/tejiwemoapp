package com.example

import com.example.data.NetworkConnectivityObserver
import com.example.ui.DinoyoConfig
import com.example.ui.DinoyoViewModel
import com.example.ui.WebContentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DinoyoViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private class FakeNetworkMonitor(initialOnline: Boolean) : NetworkConnectivityObserver {
    val flow = MutableStateFlow(initialOnline)
    override val isOnline = flow
    override fun currentOnlineState(): Boolean = flow.value
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state starts loading Dinoyo home URL`() = runTest {
    val viewModel = DinoyoViewModel(FakeNetworkMonitor(initialOnline = true))
    val state = viewModel.uiState.value
    assertEquals(DinoyoConfig.HOME_URL, state.currentUrl)
    assertEquals(WebContentState.InitialLoading, state.contentState)
    assertTrue(state.isOnline)
  }

  @Test
  fun `progress and page finish transition content to Ready`() = runTest {
    val viewModel = DinoyoViewModel(FakeNetworkMonitor(initialOnline = true))
    viewModel.onProgressChanged(90)
    assertTrue(viewModel.uiState.value.contentState is WebContentState.Ready)
    assertTrue(viewModel.uiState.value.hasLoadedOnce)

    viewModel.onPageFinished(
      url = "https://dinoyo.vercel.app/explore",
      title = "Dinoyo Explore",
      canGoBack = true,
      canGoForward = false,
    )
    val state = viewModel.uiState.value
    assertEquals("https://dinoyo.vercel.app/explore", state.currentUrl)
    assertEquals("Dinoyo Explore", state.pageTitle)
    assertTrue(state.canGoBack)
    assertFalse(state.canGoForward)
    assertFalse(state.isLoading)
  }

  @Test
  fun `offline error recovers automatically when network returns`() = runTest {
    val networkMonitor = FakeNetworkMonitor(initialOnline = false)
    val viewModel = DinoyoViewModel(networkMonitor)
    advanceUntilIdle()

    viewModel.onMainFrameError(-2, "ERR_INTERNET_DISCONNECTED")
    val errorState = viewModel.uiState.value.contentState as WebContentState.Error
    assertTrue(errorState.isOffline)

    networkMonitor.flow.value = true
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isOnline)
    assertTrue(viewModel.uiState.value.contentState is WebContentState.InitialLoading)
  }
}
