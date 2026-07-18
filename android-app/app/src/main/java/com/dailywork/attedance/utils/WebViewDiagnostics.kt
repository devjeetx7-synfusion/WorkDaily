package com.dailywork.attedance.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.UserMessagingPlatform
import com.dailywork.attedance.BuildConfig
import com.dailywork.attedance.ads.AdConfiguration

object WebViewDiagnostics {

    fun getDiagnosticsInfo(context: Context): Map<String, String> {
        val diagnostics = mutableMapOf<String, String>()

        // 1. WebView Package Name & Version
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val webViewPackage = android.webkit.WebView.getCurrentWebViewPackage()
                diagnostics["WebView Package"] = webViewPackage?.packageName ?: "None"
                diagnostics["WebView Version"] = webViewPackage?.versionName ?: "None"
            } else {
                val clazz = Class.forName("android.webkit.WebViewFactory")
                val method = clazz.getMethod("getLoadedPackageInfo")
                val packageInfo = method.invoke(null) as? android.content.pm.PackageInfo
                diagnostics["WebView Package"] = packageInfo?.packageName ?: "None"
                diagnostics["WebView Version"] = packageInfo?.versionName ?: "None"
            }
        } catch (e: Exception) {
            diagnostics["WebView Package"] = "Error: ${e.message}"
            diagnostics["WebView Version"] = "Error: ${e.message}"
        }

        // 2. Is WebView Provider Available
        try {
            android.webkit.CookieManager.getInstance()
            diagnostics["WebView Available"] = "true"
        } catch (e: Exception) {
            diagnostics["WebView Available"] = "false (${e.message})"
        }

        // 3. Google Play Services Availability
        try {
            val api = GoogleApiAvailability.getInstance()
            val resultCode = api.isGooglePlayServicesAvailable(context)
            if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                diagnostics["Google Play Services"] = "Available"
            } else {
                diagnostics["Google Play Services"] = "Unavailable (${api.getErrorString(resultCode)})"
            }
        } catch (e: Exception) {
            diagnostics["Google Play Services"] = "Error: ${e.message}"
        }

        // 4. Network and VPN Capabilities
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val caps = cm.getNetworkCapabilities(activeNetwork)
                if (caps != null) {
                    val internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    diagnostics["Network Capability"] = "Internet: $internet, Validated: $validated, VPN: $isVpn"
                } else {
                    diagnostics["Network Capability"] = "No Capabilities found"
                }
            } else {
                diagnostics["Network Capability"] = "No Active Network"
            }
        } catch (e: Exception) {
            diagnostics["Network Capability"] = "Error: ${e.message}"
        }

        // 5. Current Process Name
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                diagnostics["Process Name"] = Application.getProcessName()
            } else {
                val pid = Process.myPid()
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val infoList = am.runningAppProcesses
                val processName = infoList?.find { it.pid == pid }?.processName ?: "Unknown"
                diagnostics["Process Name"] = processName
            }
        } catch (e: Exception) {
            diagnostics["Process Name"] = "Error: ${e.message}"
        }

        // 6. Mobile Ads SDK Version
        try {
            diagnostics["Mobile Ads SDK Version"] = MobileAds.getVersion().toString()
        } catch (e: Exception) {
            diagnostics["Mobile Ads SDK Version"] = "Error: ${e.message}"
        }

        // 7. Build Type and Test Mode Status
        diagnostics["Build Info"] = "Build: ${BuildConfig.BUILD_TYPE}, TestMode: ${AdConfiguration.isTestMode}"

        // 8. Consent Status & canRequestAds
        try {
            val consentInfo = UserMessagingPlatform.getConsentInformation(context)
            val consentStatus = when (consentInfo.consentStatus) {
                com.google.android.ump.ConsentInformation.ConsentStatus.UNKNOWN -> "Unknown"
                com.google.android.ump.ConsentInformation.ConsentStatus.NOT_REQUIRED -> "Not Required"
                com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED -> "Required"
                com.google.android.ump.ConsentInformation.ConsentStatus.OBTAINED -> "Obtained"
                else -> "Unknown"
            }
            diagnostics["Consent Status"] = "Status: $consentStatus, CanRequestAds: ${consentInfo.canRequestAds()}"
        } catch (e: Exception) {
            diagnostics["Consent Status"] = "Error: ${e.message}"
        }

        return diagnostics
    }

    fun logDiagnostics(context: Context) {
        val info = getDiagnosticsInfo(context)
        Log.d("WebViewDiagnostics", "=== WebView & Environment Diagnostics ===")
        info.forEach { (key, value) ->
            Log.d("WebViewDiagnostics", "$key: $value")
        }
    }
}
