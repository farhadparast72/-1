# Pro Android Shell (Material) — Build APK بدون Android Studio

این پروژه یک پوسته‌ی **کاملاً Native** با طراحی **Material** می‌سازد (Toolbar + BottomNav + FAB + Splash + Pull-to-refresh)
و محتوای اصلی را از فایل `app/src/main/assets/index.html` داخل WebView اجرا می‌کند.

## ساختار مهم
- `.github/workflows/build-apk.yml` : ساخت خودکار APK روی GitHub Actions
- `app/src/main/assets/index.html` : همین فایل HTML شما

## گرفتن APK (بدون Android Studio)
1) یک Repo جدید در GitHub بساز  
2) **محتویات این پوشه** را کامل آپلود کن (نه خود ZIP)  
   حتماً در ریشه‌ی Repo این‌ها را ببینی:  
   - `app/`  
   - `.github/`  
   - `build.gradle.kts`  
   - `settings.gradle.kts`

3) برو تب **Actions** → Workflow **Build APK (Debug)** → **Run workflow**
4) بعد از سبز شدن ✅، پایین همان Run بخش **Artifacts** ظاهر می‌شود → `app-debug-apk`

## خطاهای رایج و راه‌حل سریع
- **Artifacts خالی است:** یعنی Build موفق نشده یا مرحله Upload اجرا نشده. لاگ مرحله Build را ببین.
- **Invalid workflow / YAML:** داخل `.github/workflows/` فقط همین `build-apk.yml` را نگه دار. بقیه (مثل jekyll) را حذف کن.
- **gradlew پیدا نشد:** این پروژه عمداً Wrapper ندارد؛ Workflow خودش Gradle را دانلود می‌کند تا گیر نکنی.
- **SDK/Build-tools mismatch:** اگر خطا گفت SDK پیدا نشد، در workflow مقدار `platforms;android-34` و `build-tools;34.0.0` را مطابق `compileSdk` تغییر بده.
