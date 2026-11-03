# JARVIS AI (`com.aistudio.adaassistant.jarvis`) — Signing Key & Self-Update Guide

## 1. Saved Signing Key Certificate (For Future Updates & Firebase Auth)

The persistent signing keystore is saved in **`signing/jarvis-signing-key.b64`** so it is exported with the repository to GitHub.

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

## 2. GitHub Actions Signing Modes (`/.github/workflows/android-release.yml`)

1. **Custom Release Signing via GitHub Secrets (Optional)**:
   If you configure `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` in **GitHub Repository Settings → Secrets and variables → Actions**, the workflow runs `gradle assembleRelease` using your custom upload key (`alias: upload`).
2. **Automatic Signed Debug Fallback (Default — Zero Setup Required)**:
   If release signing secrets are not set, the workflow automatically restores the keystore from `signing/jarvis-signing-key.b64` (or generates a valid `debug.keystore` via `keytool` if absent) and runs `gradle assembleDebug`, packaging `JARVIS-AI-v1.1.1.apk` and `version.json` into the GitHub Release without failing.
