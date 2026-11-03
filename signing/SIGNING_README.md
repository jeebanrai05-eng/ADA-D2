# JARVIS AI (`com.aistudio.adaassistant.jarvis`) — Signing Key & Self-Update Guide

## 1. Saved Signing Key Certificate (For Future Updates & Firebase Auth)

To ensure that future versions (`v1.1.2`, `v1.2.0`, etc.) install seamlessly on top of `v1.1.1` without Android showing **"App not installed as package conflicts with an existing package"**, every build must use the **same signing key**.

The signing keystore is already saved in the repository root as **`debug.keystore.base64`**.

### Certificate Fingerprints
- **Package Name (`applicationId`)**: `com.aistudio.adaassistant.jarvis`
- **Version Name**: `1.1.1`
- **Version Code**: `111`
- **SHA-1 Fingerprint**:
  ```text
  1A:8D:F9:BB:89:5C:A3:A4:A4:80:91:9F:45:A9:B2:31:4D:08:1B:78
  ```
- **SHA-256 Fingerprint**:
  ```text
  09:08:34:A8:C9:F8:AD:9F:B6:9B:C0:08:2F:8D:DD:B1:B3:51:CD:3F:B9:07:ED:FB:82:3C:57:A0:B9:D9:49:7D
  ```
- **Keystore Alias**: `androiddebugkey`
- **Store Password / Key Password**: `android`

---

## 2. How to Restore the Keystore Locally or in CI/CD

```bash
base64 -d debug.keystore.base64 > debug.keystore
```

Verify the restored certificate fingerprints:
```bash
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

---

## 3. GitHub Actions Automatic Release (`/.github/workflows/android-release.yml`)

When you push this project to GitHub:
1. Pushing a tag like `v1.1.1` (or triggering **Actions → JARVIS Android Release & OTA Update → Run workflow**) automatically:
   - Restores `debug.keystore` from `debug.keystore.base64`.
   - Verifies the `SHA-1` and `SHA-256` signing certificate.
   - Builds the signed APK (`JARVIS-AI-v1.1.1.apk`).
   - Updates `version.json` with your exact GitHub repository release download URL and APK SHA-256 checksum.
   - Creates a **GitHub Release (`v1.1.1`)** with `JARVIS-AI-v1.1.1.apk` and `version.json` attached.

## 4. How Self-Update Works for Future Versions (`v1.1.2+`)

1. Bump `versionCode` (e.g. `112`) and `versionName` (e.g. `"1.1.2"`) in `app/build.gradle.kts` and `version.json`.
2. Push to GitHub or tag `v1.1.2`.
3. Inside the **JARVIS AI** app, open **Sidebar → API Configuration & Secrets** (or **Smart Home & Workshop IoT → OTA Self-Update & Signing Manager**), enter your GitHub repo (`owner/repo`), and tap **Check for Updates**.
4. JARVIS fetches `version.json` / GitHub Releases, verifies that `versionCode > 111` and that the signing certificate SHA-256 matches, and opens the signed APK download so Android upgrades the app in-place while preserving all user data!
