package com.keeftalk.chat.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LanDiscoveryManager(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_keeftalk._tcp."
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerService(userName: String, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Keeftalk-$userName"
            setServiceType(serviceType)
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d("LanDiscovery", "Service registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("LanDiscovery", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d("LanDiscovery", "Service unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("LanDiscovery", "Unregistration failed: $errorCode")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterService() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
            registrationListener = null
        }
    }

    fun discoverPeers(): Flow<NsdServiceInfo> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("LanDiscovery", "Discovery started")
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                Log.d("LanDiscovery", "Service found: ${info.serviceName}")
                // Service type might have a dot at the end depending on the system
                if (info.serviceType.contains(serviceType.removeSuffix("."))) {
                    nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(resolveInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e("LanDiscovery", "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                            Log.d("LanDiscovery", "Service resolved: ${resolvedServiceInfo.host.hostAddress}")
                            trySend(resolvedServiceInfo)
                        }
                    })
                }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                Log.d("LanDiscovery", "Service lost: ${info.serviceName}")
            }

            override fun onDiscoveryStopped(regType: String) {
                Log.d("LanDiscovery", "Discovery stopped")
            }

            override fun onStartDiscoveryFailed(type: String, errorCode: Int) {
                Log.e("LanDiscovery", "Start discovery failed: $errorCode")
                close()
            }

            override fun onStopDiscoveryFailed(type: String, errorCode: Int) {
                Log.e("LanDiscovery", "Stop discovery failed: $errorCode")
                close()
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }
}
