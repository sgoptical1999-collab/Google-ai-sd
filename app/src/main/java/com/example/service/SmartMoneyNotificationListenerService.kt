package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.SmartMoneyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SmartMoneyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        // Never process notifications from our own app or system UI noise
        if (packageName == applicationContext.packageName ||
            packageName == "android" ||
            packageName == "com.android.systemui"
        ) {
            return
        }

        val notification = sbn.notification ?: return
        // Skip ongoing silent notifications (media players, download progress, etc.)
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        val body = listOf(bigText, text, subText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")

        if (title.isBlank() && body.isBlank()) return

        val appContainer = (application as? SmartMoneyApplication)?.container ?: return
        val timestamp = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        val resolvedLabel = InstalledPaymentAppsHelper.resolveAppLabel(applicationContext, packageName)

        serviceScope.launch {
            runCatching {
                appContainer.transactionRepository.ingestRawNotification(
                    packageName = packageName,
                    title = title,
                    body = body,
                    timestamp = timestamp,
                    bypassMonitoringToggle = false,
                    resolvedAppLabel = resolvedLabel
                )
            }.onFailure { err ->
                Log.w("SmartMoneyListener", "Notification ingestion skipped: ${err.message}")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
