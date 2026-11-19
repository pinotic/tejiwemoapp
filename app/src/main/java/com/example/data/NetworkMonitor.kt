package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkConnectivityObserver {
  val isOnline: Flow<Boolean>
  fun currentOnlineState(): Boolean
}

class AndroidNetworkMonitor(context: Context) : NetworkConnectivityObserver {
  private val connectivityManager =
    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  override fun currentOnlineState(): Boolean {
    val cm = connectivityManager ?: return true
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  override val isOnline: Flow<Boolean> = callbackFlow {
    val cm = connectivityManager
    if (cm == null) {
      trySend(true)
      close()
      return@callbackFlow
    }

    trySend(currentOnlineState())

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
      }

      override fun onLost(network: Network) {
        trySend(currentOnlineState())
      }

      override fun onUnavailable() {
        trySend(false)
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    runCatching { cm.registerNetworkCallback(request, callback) }
      .onFailure { trySend(currentOnlineState()) }

    awaitClose {
      runCatching { cm.unregisterNetworkCallback(callback) }
    }
  }.distinctUntilChanged()
}
