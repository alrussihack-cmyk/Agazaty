# Agazaty | إجازاتي

Native Android employee leave and shift-planning app built with Kotlin, Jetpack Compose and Room.

## Visual identity
Applied from the six reference screens (colours sampled from the PNGs):

| Token | Value | Used for |
|---|---|---|
| Cream | `#FFF8F1` | screen background |
| Primary | `#C2410C` | hero card, selected pills, CTA, FAB, active nav |
| Primary dark | `#9A3412` | text on pale orange |
| Pale | `#FFEDD5` | avatars, "في إجازة" badge, autofill card, month chip |
| Peach | `#FDBA74` | chart bars (peak month uses Primary) |
| Ink / Muted | `#2B1B10` / `#6B5645` | titles / secondary text |
| Border | `#F3E3D3` | 1dp hairline on every card, input and pill |
| Mint · Sky · Stone | `#CCFBF1/#115E59` · `#DBEAFE/#1E40AF` · `#E7E5E4/#44403C` | returned ≤30 days · returned >30 days · not recorded |

Typeface: **Cairo** (SIL OFL, bundled in `res/font`, weights 400–900). Layout is forced RTL, Latin digits throughout. Shapes: cards 22dp, hero 28dp, pills fully rounded, CTA 24dp. Tokens live in `ui/theme/Theme.kt`; shared components in `ui/components/Components.kt`.

## Screens
1. **لوحة الإجازات** – hero count, "returning this week" chip, four metrics, monthly returns chart (half-year window), returning-soon list.
2. **سجل الإجازات** – search + filters (الكل / في إجازة / عادوا), tap a row to edit.
3. **إضافة / تعديل إجازة** – employee type-ahead with autofill card, leave type, date pickers, optional actual return, notes, live status, delete.
4. **الموظفون** – search, filters with counts (الكل / Life Guard / فنيو الحمامات), add employee from the ＋ button.
5. **ملف الموظف** – stats, leave history, add leave for this employee.
6. **توزيع الشيفتات** – month chip, three 10-day shifts, six area cards, total, assign form, tap an area to see/remove members.

## Behaviour
- Offline-first Room database (version 3). First run imports the 47 employees and 27 leaves from `seed.json`.
- Leave status is always derived: a leave with a start date and no actual return date is *في إجازة* until the return is recorded; a recorded return shows *عاد منذ N يومًا* (mint ≤30 days, blue beyond). Imported rows whose return date has already passed are stored as actual returns.
- Migration 2→3 adds `type` (default `سنوية`, the workbook has no leave type) and `actualReturnDate`; existing data is kept.
- Shift assignment rejects overlapping leave, duplicates in the same shift, and exceeding an area's capacity.

## Build
Open the repository in Android Studio (JDK 17), sync and run `app`, or from a shell:

```
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions builds the debug APK on every push and pull request to `main` and uploads it as the `agazaty-debug-apk` artifact. Requires Android SDK platform 35.

## Data and privacy
All employee, leave, and shift-assignment data is stored locally on the device. The app does not require an account, server, or internet connection for normal operation. The seed data is bundled with the app.

## Known follow-up work
- Export / print reports.
- Signed release build and Play listing.
- Unit tests for `domain/LeaveLogic.kt` and instrumented UI tests.

The source workbook contains no hiring dates and no leave types; none are fabricated by the app beyond the default leave type noted above.
