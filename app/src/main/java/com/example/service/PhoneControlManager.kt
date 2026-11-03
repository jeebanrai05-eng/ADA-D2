package com.example.service

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat

data class BatteryTelemetry(
    val percentage: Int,
    val isCharging: Boolean,
    val powerSource: String,
    val temperatureC: Float,
    val voltageMv: Int
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class VolumeLevels(
    val mediaCurrent: Int,
    val mediaMax: Int,
    val ringCurrent: Int,
    val ringMax: Int,
    val alarmCurrent: Int,
    val alarmMax: Int
)

class PhoneControlManager(private val context: Context) {

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun readBatteryTelemetry(): BatteryTelemetry {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 85

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charge"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Data Bus"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Inductive Wireless"
            else -> if (isCharging) "External Power" else "Battery Cell"
        }

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4120) ?: 4120

        return BatteryTelemetry(
            percentage = pct,
            isCharging = isCharging,
            powerSource = source,
            temperatureC = tempTenths / 10f,
            voltageMv = voltage
        )
    }

    fun isNetworkOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun hasFlashlightHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun setFlashlight(enabled: Boolean): Result<Boolean> {
        if (!hasFlashlightHardware()) {
            return Result.failure(IllegalStateException("This device does not report a hardware camera flash unit."))
        }
        val cm = cameraManager
            ?: return Result.failure(IllegalStateException("CameraManager service unavailable."))
        return try {
            val torchCameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cm.cameraIdList.firstOrNull()
                ?: return Result.failure(IllegalStateException("No camera with flash unit found on device."))

            cm.setTorchMode(torchCameraId, enabled)
            Result.success(enabled)
        } catch (se: SecurityException) {
            Result.failure(SecurityException("Camera permission required by Android to control the flashlight."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readVolumeLevels(): VolumeLevels {
        val am = audioManager ?: return VolumeLevels(10, 15, 5, 7, 6, 7)
        return VolumeLevels(
            mediaCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC),
            mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1),
            ringCurrent = am.getStreamVolume(AudioManager.STREAM_RING),
            ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1),
            alarmCurrent = am.getStreamVolume(AudioManager.STREAM_ALARM),
            alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        )
    }

    fun setStreamVolume(streamType: Int, value: Int): VolumeLevels {
        try {
            audioManager?.setStreamVolume(streamType, value, 0)
        } catch (_: SecurityException) {
            // Do Not Disturb policy may block ring volume change
        }
        return readVolumeLevels()
    }

    fun dispatchMediaCommand(command: String): Result<String> {
        val am = audioManager ?: return Result.failure(IllegalStateException("AudioManager unavailable"))
        val keyCode = when (command.uppercase()) {
            "PLAY_PAUSE", "PLAY", "PAUSE" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "NEXT" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "PREVIOUS", "PREV" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "STOP" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val now = SystemClock.uptimeMillis()
        val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
        am.dispatchMediaKeyEvent(downEvent)
        am.dispatchMediaKeyEvent(upEvent)
        return Result.success("Dispatched media key command: $command")
    }

    fun getInstalledLaunchableApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }
        return resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = try {
                ri.loadLabel(pm).toString().trim()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }
            InstalledAppInfo(appName = label.ifBlank { pkg }, packageName = pkg)
        }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun launchAppByPackageOrName(queryOrPackage: String): Result<String> {
        val pm = context.packageManager
        val trimmed = queryOrPackage.trim()
        // Direct package match first
        val directIntent = pm.getLaunchIntentForPackage(trimmed)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            return Result.success("Launched package $trimmed")
        }

        // Well-known app shortcuts
        val lower = trimmed.lowercase()
        when {
            "youtube" in lower -> {
                val ytPkg = pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (ytPkg != null) {
                    ytPkg.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(ytPkg)
                    return Result.success("Launched YouTube")
                }
                val webYt = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webYt)
                return Result.success("Opened YouTube in browser")
            }
            "whatsapp" in lower -> {
                val waPkg = pm.getLaunchIntentForPackage("com.whatsapp")
                if (waPkg != null) {
                    waPkg.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(waPkg)
                    return Result.success("Launched WhatsApp")
                }
                val webWa = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webWa)
                return Result.success("Opened WhatsApp Web Link")
            }
            "setting" in lower -> {
                return openSystemSettings("MAIN")
            }
            "chrome" in lower || "browser" in lower -> {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                return Result.success("Opened Web Browser")
            }
        }

        // Search installed apps by label
        val matched = getInstalledLaunchableApps().firstOrNull {
            it.appName.contains(trimmed, ignoreCase = true) ||
                it.packageName.contains(trimmed, ignoreCase = true)
        }
        if (matched != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return Result.success("Launched ${matched.appName}")
            }
        }
        return Result.failure(IllegalStateException("Application '$trimmed' is not installed or launchable on this device."))
    }

    fun openAppDetailsSettings(packageName: String = context.packageName): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened App Info settings for $packageName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openSystemSettings(section: String): Result<String> {
        val action = when (section.uppercase()) {
            "WIFI", "WI-FI" -> Settings.ACTION_WIFI_SETTINGS
            "BLUETOOTH" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "DISPLAY" -> Settings.ACTION_DISPLAY_SETTINGS
            "SOUND", "AUDIO" -> Settings.ACTION_SOUND_SETTINGS
            "NOTIFICATION_LISTENER" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            "ACCESSIBILITY" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Android $section Settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun initiateDialCall(phoneNumber: String): Result<String> {
        val cleaned = phoneNumber.trim()
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(cleaned)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Phone Dialer for $cleaned")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun composeSmsMessage(recipient: String, body: String): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(recipient.trim())}")
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened SMS composer for $recipient")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareTextViaSharesheet(text: String, title: String = "Share via JARVIS"): Result<String> {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success("Opened Android Sharesheet")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setSystemAlarm(hour: Int, minute: Int, label: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "JARVIS Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Alarm configured for %02d:%02d (%s)".format(hour, minute, label))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openGoogleWebSearch(query: String): Result<String> {
        val raw = query.trim()
        val url = if (raw.startsWith("http://") || raw.startsWith("https://")) {
            raw
        } else {
            "https://www.google.com/search?q=${Uri.encode(raw)}"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                putExtra(SearchManager.QUERY, raw)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened browser search for '$raw'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
