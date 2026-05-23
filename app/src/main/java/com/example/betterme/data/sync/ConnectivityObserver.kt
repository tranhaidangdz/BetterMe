package com.example.betterme.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

/**
 * Cold [Flow] of "is the device currently online with usable internet?".
 *
 * Backed by [ConnectivityManager.registerNetworkCallback], which fires on:
 *  - cellular / wifi gain or loss
 *  - VPN attach
 *  - validated capability changes (a network with no real internet won't emit `true`)
 *
 * The flow re-emits only on state changes ([distinctUntilChanged]) so collectors can
 * `if (online) launchSync()` without needing their own debouncing. On first collection
 * it emits the current state synchronously via [onStart] so callers aren't blocked on
 * the first network callback when the device is already connected.
 *
 * Lifecycle-safe: the callback is unregistered when the Flow is cancelled.
 */
class ConnectivityObserver(private val context: Context) {

    /** Snapshot read — used by code paths that can't suspend. */
    fun isOnlineNow(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun observe(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isOnlineNow())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val online = networkCapabilities
                    .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(online)
            }
        }
        cm.registerNetworkCallback(request, callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
        .onStart { emit(isOnlineNow()) }
        .distinctUntilChanged()
}
