package com.automod.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automod.ai.service.AnalysisService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Auto-start background service if needed
            // Optionally start Frida server or other initialization
        }
    }
}
