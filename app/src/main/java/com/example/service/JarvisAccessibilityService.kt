package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // JARVIS never collects private screen content or keystrokes.
        // Only tracks connection state for explicit user-authorized navigation commands.
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _isConnected.value = false
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile
        private var instance: JarvisAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _emergencyDisabled = MutableStateFlow(false)
        val emergencyDisabled: StateFlow<Boolean> = _emergencyDisabled.asStateFlow()

        fun setEmergencyDisabled(disabled: Boolean) {
            _emergencyDisabled.value = disabled
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            return enabledServices.contains(context.packageName, ignoreCase = true)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun performPermittedAction(actionName: String): Result<String> {
            if (_emergencyDisabled.value) {
                return Result.failure(IllegalStateException("Emergency Automation Disable Switch is ACTIVE. Automation blocked."))
            }
            val service = instance
                ?: return Result.failure(IllegalStateException("JARVIS Accessibility Service is not enabled in Android Settings."))

            val globalAction = when (actionName.uppercase()) {
                "BACK" -> GLOBAL_ACTION_BACK
                "HOME" -> GLOBAL_ACTION_HOME
                "RECENTS" -> GLOBAL_ACTION_RECENTS
                "NOTIFICATIONS" -> GLOBAL_ACTION_NOTIFICATIONS
                "QUICK_SETTINGS" -> GLOBAL_ACTION_QUICK_SETTINGS
                else -> return Result.failure(IllegalArgumentException("Unsupported accessibility workflow: $actionName"))
            }

            val ok = service.performGlobalAction(globalAction)
            return if (ok) {
                Result.success("Executed Accessibility Action: $actionName")
            } else {
                Result.failure(IllegalStateException("Android rejected accessibility action: $actionName"))
            }
        }
    }
}
