package com.example.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSummary(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class JarvisNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (_masterStopActive.value) return
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        if (_masterStopActive.value) return
        try {
            val active = activeNotifications ?: return
            val pm = applicationContext.packageManager
            val allowedApps = _allowedPackageFilter.value
            val items = active.mapNotNull { sbn ->
                if (sbn.packageName == applicationContext.packageName) return@mapNotNull null
                if (allowedApps.isNotEmpty() && sbn.packageName !in allowedApps) return@mapNotNull null
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                if (title.isBlank() && text.isBlank()) return@mapNotNull null
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    sbn.packageName.substringAfterLast('.')
                }
                NotificationSummary(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = appLabel,
                    title = title.ifBlank { appLabel },
                    text = text,
                    postTime = sbn.postTime
                )
            }.sortedByDescending { it.postTime }.take(20)
            _notifications.value = items
        } catch (_: Exception) {
            // Ignore security exceptions when listener is not bound
        }
    }

    companion object {
        private val _notifications = MutableStateFlow<List<NotificationSummary>>(emptyList())
        val notifications: StateFlow<List<NotificationSummary>> = _notifications.asStateFlow()

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _allowedPackageFilter = MutableStateFlow<Set<String>>(emptySet())
        val allowedPackageFilter: StateFlow<Set<String>> = _allowedPackageFilter.asStateFlow()

        private val _masterStopActive = MutableStateFlow(false)

        fun setMasterStop(stopped: Boolean) {
            _masterStopActive.value = stopped
            if (stopped) {
                _notifications.value = emptyList()
            }
        }

        fun toggleAllowedPackage(packageName: String) {
            val current = _allowedPackageFilter.value.toMutableSet()
            if (packageName in current) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            _allowedPackageFilter.value = current
        }

        fun clearCapturedNotifications() {
            _notifications.value = emptyList()
        }

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            return enabledListeners.contains(context.packageName)
        }

        fun openNotificationListenerSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
