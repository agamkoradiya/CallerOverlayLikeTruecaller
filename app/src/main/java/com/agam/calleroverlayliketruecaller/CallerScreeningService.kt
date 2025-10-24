package com.agam.calleroverlayliketruecaller

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat

private const val TAG = "CallerService"

class CallerScreeningService : CallScreeningService() {

    companion object {
        const val INCOMING_NUMBER = "incoming_number"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val phoneNumber = extractPhoneNumber(callDetails)?.filter { it.isDigit() }
            Log.d(TAG, "onScreenCall: $phoneNumber")
            if (!phoneNumber.isNullOrBlank() && Settings.canDrawOverlays(this)) {
                startCallerOverlayServiceIfNotRunning(applicationContext, phoneNumber)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val callResponse = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, callResponse)
    }

    private fun extractPhoneNumber(callDetails: Call.Details): String? {
        val handle = callDetails.handle
        if (handle != null) {
            // Attempt to extract phone number from the handle
            return handle.schemeSpecificPart
        } else {
            // Handle is null, try other methods if available
            val gatewayInfo = callDetails.gatewayInfo
            if (gatewayInfo != null) {
                return gatewayInfo.originalAddress.schemeSpecificPart
            }
        }
        return null
    }

    private fun startCallerOverlayServiceIfNotRunning(context: Context, phoneNumber: String) {
        val popupIntent = Intent(context, CallerOverlayService::class.java).apply {
            putExtra(INCOMING_NUMBER, phoneNumber)
        }
        ContextCompat.startForegroundService(context, popupIntent)
    }
}