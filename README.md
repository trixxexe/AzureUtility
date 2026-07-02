# AZURE UTILITY
Developer toolkit Android app by Trixx / Azure.

## Tools
- QRFORGE — QR & barcode generator + scanner
- JSONLENS — JSON tree viewer
- MARKITDOWN — Markdown editor with PDF export
- CODEEDITOR — Syntax-highlighted code editor (25+ languages)
- TEXTPAD — Plain text editor

## Build
Requires JDK 17+. Clone and run:
    ./gradlew assembleDebug

APK output: app/build/outputs/apk/debug/app-debug.apk

## Icons
Legacy PNG icons (mipmap-hdpi through mipmap-xxxhdpi) should be generated
using Android Studio → File → New → Image Asset → select ic_launcher_foreground.xml

## CI
GitHub Actions workflow builds debug APK on every push.
Release APK requires keystore secrets configured in repository settings.
