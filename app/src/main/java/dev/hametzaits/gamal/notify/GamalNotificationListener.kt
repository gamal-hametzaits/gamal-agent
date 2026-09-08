package dev.hametzaits.gamal.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.hametzaits.gamal.GamalApp
import dev.hametzaits.gamal.data.AppDatabase
import dev.hametzaits.gamal.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Notification ingestion. Android only delivers notifications to this service
 * after the user explicitly grants Notification Listener access in system
 * settings. Without that grant this service never fires. There is no mic
 * access anywhere in this app - by design.
 */
class GamalNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return
        val prefs = GamalApp.prefs(this)
        if (!prefs.getBoolean(GamalApp.PREF_LEARN_NOTIFICATIONS, true)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        scope.launch {
            db.notificationDao().insert(
                NotificationEntity(
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Keep a rolling 7-day archive on device.
            db.notificationDao().pruneBefore(
                System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
