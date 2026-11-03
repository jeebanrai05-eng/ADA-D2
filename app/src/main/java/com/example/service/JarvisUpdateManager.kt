package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReleaseManifest(
    val versionName: String,
    val versionCode: Int,
    val releaseTag: String,
    val releaseDate: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val certSha1: String,
    val certSha256: String,
    val changelog: List<String>,
    val isUpdateAvailable: Boolean,
    val isSignatureCompatible: Boolean,
    val sourceLabel: String
)

class JarvisUpdateManager(private val context: Context) {

    val installedVersionName: String = BuildConfig.VERSION_NAME
    val installedVersionCode: Int = BuildConfig.VERSION_CODE
    val packageName: String = context.packageName

    fun readRuntimeSigningFingerprints(): Pair<String, String> {
        return try {
            val pm = context.packageManager
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signers = info.signingInfo?.apkContentsSigners
                signers?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }

            if (certBytes != null) {
                val sha1 = MessageDigest.getInstance("SHA-1").digest(certBytes)
                    .joinToString(":") { "%02X".format(it) }
                val sha256 = MessageDigest.getInstance("SHA-256").digest(certBytes)
                    .joinToString(":") { "%02X".format(it) }
                sha1 to sha256
            } else {
                DEFAULT_SHA1 to DEFAULT_SHA256
            }
        } catch (_: Exception) {
            DEFAULT_SHA1 to DEFAULT_SHA256
        }
    }

    suspend fun checkUpdateManifest(githubRepoOrUrl: String): Result<ReleaseManifest> = withContext(Dispatchers.IO) {
        val trimmed = githubRepoOrUrl.trim().ifEmpty { "jeebanraiff05/jarvis-ai" }
        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://raw.githubusercontent.com/$trimmed/main/version.json"
        }

        val (installedSha1, installedSha256) = readRuntimeSigningFingerprints()

        try {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext Result.success(
                    parseManifestJson(rawJson, installedSha256, "GitHub Live ($targetUrl)")
                )
            }
        } catch (_: Exception) {
            // Fallback to bundled assets/version.json when offline or before repo is pushed
        }

        return@withContext try {
            val bundledJson = context.assets.open("version.json").bufferedReader().use { it.readText() }
            Result.success(
                parseManifestJson(bundledJson, installedSha256, "Bundled Release Manifest (version.json)")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseManifestJson(
        jsonStr: String,
        installedSha256: String,
        sourceLabel: String
    ): ReleaseManifest {
        val json = JSONObject(jsonStr)
        val versionName = json.optString("versionName", "1.1.1")
        val versionCode = json.optInt("versionCode", 111)
        val releaseTag = json.optString("releaseTag", "v$versionName")
        val releaseDate = json.optString("releaseDate", "2026-11-03")
        val downloadUrl = json.optString(
            "downloadUrl",
            "https://github.com/jeebanraiff05/jarvis-ai/releases/download/$releaseTag/JARVIS-AI-$releaseTag.apk"
        )
        val releasePageUrl = json.optString(
            "releasePageUrl",
            "https://github.com/jeebanraiff05/jarvis-ai/releases/tag/$releaseTag"
        )

        val certObj = json.optJSONObject("signingCertificate")
        val certSha1 = certObj?.optString("sha1", DEFAULT_SHA1) ?: DEFAULT_SHA1
        val certSha256 = certObj?.optString("sha256", DEFAULT_SHA256) ?: DEFAULT_SHA256

        val changelogArr = json.optJSONArray("changelog")
        val changelogList = mutableListOf<String>()
        if (changelogArr != null) {
            for (i in 0 until changelogArr.length()) {
                changelogList.add(changelogArr.optString(i))
            }
        }

        val isUpdateAvailable = versionCode > installedVersionCode ||
            (versionName != installedVersionName && versionCode >= installedVersionCode)
        val isSignatureCompatible = certSha256.equals(installedSha256, ignoreCase = true)

        return ReleaseManifest(
            versionName = versionName,
            versionCode = versionCode,
            releaseTag = releaseTag,
            releaseDate = releaseDate,
            downloadUrl = downloadUrl,
            releasePageUrl = releasePageUrl,
            certSha1 = certSha1,
            certSha256 = certSha256,
            changelog = changelogList,
            isUpdateAvailable = isUpdateAvailable,
            isSignatureCompatible = isSignatureCompatible,
            sourceLabel = sourceLabel
        )
    }

    fun openReleaseDownloadInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        const val DEFAULT_SHA1 = "1A:8D:F9:BB:89:5C:A3:A4:A4:80:91:9F:45:A9:B2:31:4D:08:1B:78"
        const val DEFAULT_SHA256 = "09:08:34:A8:C9:F8:AD:9F:B6:9B:C0:08:2F:8D:DD:B1:B3:51:CD:3F:B9:07:ED:FB:82:3C:57:A0:B9:D9:49:7D"
    }
}
