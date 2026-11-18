# Agazaty | إجازاتي

Native Android employee leave and shift-planning app built with Kotlin, Jetpack Compose and Room.

## Implemented
- Arabic right-to-left interface inspired by the supplied six-screen prototype.
- Offline-first local Room database.
- First-run import from the supplied `employee.xlsx` workbook: all 47 employee records and 27 historical leave records.
- Employee directory with search by employee name, code, or job title.
- Leave register with employee code, dates and notes; add employees and leave entries.
- Dashboard showing active leave counts, employee totals and leave history.
- Shift planner for the current month: 1–10, 11–20, and 21–month-end.
- Assign employees to Kafkas Pool, Pool Bar, Tirana Main Pool, Tirana Family Pool, Blue Planet Pool, Relax Pool, or Technician.
- Shift assignment validation prevents assigning an employee with overlapping leave, duplicate assignment in the same shift, or exceeding the staffing count from the workbook.
- Room migration adds shift assignments without deleting existing employee/leave data.

## Build
Open the repository in Android Studio, sync Gradle and run the `app` configuration.

The GitHub Actions workflow builds a debug APK on pushes and pull requests to `main`, then uploads it as the `agazaty-debug-apk` workflow artifact.

## Data and privacy
All employee, leave, and shift-assignment data is stored locally on the device. The app does not require an account, server, or internet connection for normal operation. The seed data is bundled with the app.

## Known follow-up work
- Date picker controls and localized date formatting.
- Employee profile detail screen and leave-status filtering.
- Shift assignment editing and export/print reports.
- Full device/emulator QA and signed release build.

The source workbook contains no hiring dates; none are fabricated by the app.
