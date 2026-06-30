package com.automod.ai.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.automod.ai.R
import com.automod.ai.analyzer.DynamicAnalyzer
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class AnalysisService : Service() {
    
    companion object {
        const val CHANNEL_ID = "automod_analysis"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_ANALYSIS = "com.automod.ai.START_ANALYSIS"
        const val ACTION_STOP = "com.automod.ai.STOP"
        const val EXTRA_PACKAGE = "extra_package"
    }

    private val analyzer: DynamicAnalyzer by inject()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ANALYSIS -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID, createNotification("Analyzing $packageName..."))
                job = scope.launch {
                    analyzer.analyzePackage(packageName)
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                job?.cancel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoMod Analysis",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background APK analysis service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoMod AI")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_analysis)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
