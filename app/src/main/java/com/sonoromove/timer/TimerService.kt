package com.sonoromove.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Mantém o processo vivo (e mostra notificação) enquanto um timer está ativo. */
class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Timer", NotificationManager.IMPORTANCE_LOW))
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, build(TimerEngine.state.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, build(TimerEngine.state.value))
        job?.cancel()
        job = scope.launch {
            var lastText = ""
            TimerEngine.state.collect { s ->
                if (!s.active) {
                    stopSelf()
                    return@collect
                }
                val text = text(s)
                if (text != lastText) {
                    lastText = text
                    nm.notify(1, build(s))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun text(s: RunState): String {
        val t = if (s.isStopwatch) formatTime((s.elapsedMs / 1000).toInt())
        else formatTime(((s.remainingMs + 999) / 1000).toInt())
        val label = s.phase?.kind?.label ?: "CRONÔMETRO"
        return "$label  $t" + if (s.running) "" else "  (pausado)"
    }

    private fun build(s: RunState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(s.timer?.name ?: "Timer Plus")
            .setContentText(text(s))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "timer"
        fun start(c: Context) = ContextCompat.startForegroundService(c, Intent(c, TimerService::class.java))
    }
}
