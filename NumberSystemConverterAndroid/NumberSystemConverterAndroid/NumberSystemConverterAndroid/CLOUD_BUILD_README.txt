NO ANDROID STUDIO NEEDED

1. Go to github.com and create a new repository.
2. Upload ALL files/folders from this NumberSystemConverterAndroid folder.
   IMPORTANT: include the hidden .github folder.
3. Commit/upload them to the main branch.
4. Open the repository's Actions tab.
5. Open "Build Android APK".
6. Click "Run workflow" if it did not start automatically.
7. When the run is green, open it and download the artifact named:
   NumberSystemConverter-APK
8. Extract the downloaded artifact ZIP. Inside is app-debug.apk.

The cloud workflow installs JDK 17, Android SDK API 35, Gradle 8.9,
builds :app:assembleDebug, and uploads the APK automatically.
