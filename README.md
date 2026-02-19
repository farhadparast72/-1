# مدیریت املاک — Ultra (Android)

Native **Material 3** shell (TopAppBar + Bottom Navigation + FAB + Splash + Pull-to-refresh) that loads your existing HTML/JS app in a WebView.

## Assumptions
- packageName: `com.example.amlakultra`
- minSdk: 26 (Android 8.0 / API 26)
- compileSdk/targetSdk: 34

## How it works
- The HTML is served from `app/src/main/assets/app/index.html` using **WebViewAssetLoader** so `localStorage` is stable.
- Native Bottom Navigation & FAB call your JS function `setActiveView('<view>')` inside the WebView.

## Build locally (no Android Studio)
1) Install **Android SDK Command Line Tools** and set:
   - `ANDROID_SDK_ROOT` (or `ANDROID_HOME`)
2) Bootstrap Gradle Wrapper (only needed if wrapper JAR is missing):
   ```bash
   bash scripts/bootstrap-gradle-wrapper.sh
   ```
3) Build APK:
   ```bash
   ./gradlew --no-daemon assembleDebug
   ```
4) APK path:
   - `app/build/outputs/apk/debug/app-debug.apk`

## CI
GitHub Actions workflow: `.github/workflows/build-apk.yml` (uploads APK as an artifact).
