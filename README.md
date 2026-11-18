# Agazaty | إجازاتي

Offline-first native Android employee and leave management app built with Kotlin, Jetpack Compose and Room.

## Features
- Arabic RTL interface based on the supplied prototype.
- Local Room database; works without internet after installation.
- Employee directory and search by name/code.
- Leave register and employee leave history.
- Add employees and leave records.
- Dashboard for active leaves, upcoming returns and return tracking.
- Initial seed data: 47 employees and 27 leave records from the supplied workbook.

## Build
Open in Android Studio, sync Gradle and run the `app` configuration. Or run `gradle assembleDebug` with Android SDK 35 and JDK 17.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Notes
Shift overview is currently a summary; per-employee shift assignment editing is not yet implemented. The project has not yet been device-tested.
