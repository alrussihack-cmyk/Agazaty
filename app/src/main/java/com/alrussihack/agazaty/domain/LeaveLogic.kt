package com.alrussihack.agazaty.domain

import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

val LEAVE_TYPES = listOf("سنوية", "طارئة", "مرضية", "أخرى")

private val MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

fun monthName(month: Int): String = MONTHS[month - 1]
fun monthLabel(ym: YearMonth): String = "${monthName(ym.monthValue)} ${ym.year}"

private fun weekdayName(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "الاثنين"
    DayOfWeek.TUESDAY -> "الثلاثاء"
    DayOfWeek.WEDNESDAY -> "الأربعاء"
    DayOfWeek.THURSDAY -> "الخميس"
    DayOfWeek.FRIDAY -> "الجمعة"
    DayOfWeek.SATURDAY -> "السبت"
    DayOfWeek.SUNDAY -> "الأحد"
}

/** "الأربعاء 18 نوفمبر 2026" — Latin digits, fixed Arabic names (independent of device locale). */
fun fullDateLabel(d: LocalDate): String = "${weekdayName(d.dayOfWeek)} ${d.dayOfMonth} ${monthName(d.monthValue)} ${d.year}"

fun parseDate(s: String): LocalDate? = if (s.isBlank()) null else runCatching { LocalDate.parse(s) }.getOrNull()

private fun two(n: Int) = n.toString().padStart(2, '0')
fun fmtDayMonth(d: LocalDate): String = "${two(d.dayOfMonth)}/${two(d.monthValue)}"
fun fmtFull(d: LocalDate): String = "${two(d.dayOfMonth)}/${two(d.monthValue)}/${d.year}"
fun fmtDayMonth(s: String): String = parseDate(s)?.let { fmtDayMonth(it) } ?: ""
fun fmtFull(s: String): String = parseDate(s)?.let { fmtFull(it) } ?: ""

/** Parses user-entered dd/MM/yyyy back to ISO, or null. */
fun parseDisplayDate(s: String): String? = runCatching {
    val (d, m, y) = s.trim().split("/").map { it.toInt() }
    LocalDate.of(y, m, d).toString()
}.getOrNull()

sealed interface LeaveStatus {
    data object OnLeave : LeaveStatus
    data object Upcoming : LeaveStatus
    data class Returned(val daysAgo: Int) : LeaveStatus
    data object NoReturn : LeaveStatus
}

/**
 * Status is always derived, never stored:
 *  - actual return date set            -> returned N days ago
 *  - start + expected return, no actual -> on leave (stays so until the return is recorded)
 *  - return date only (no start)        -> returned N days ago
 *  - nothing                            -> return not recorded
 */
fun LeaveRecord.status(today: LocalDate = LocalDate.now()): LeaveStatus {
    val start = parseDate(startDate)
    val expected = parseDate(returnDate)
    val actual = parseDate(actualReturnDate)
    return when {
        actual != null -> LeaveStatus.Returned(daysBetween(actual, today))
        start != null -> if (today.isBefore(start)) LeaveStatus.Upcoming else LeaveStatus.OnLeave
        expected != null -> LeaveStatus.Returned(daysBetween(expected, today))
        else -> LeaveStatus.NoReturn
    }
}

private fun daysBetween(from: LocalDate, to: LocalDate) = ChronoUnit.DAYS.between(from, to).toInt().coerceAtLeast(0)

fun LeaveRecord.isOnLeave(today: LocalDate = LocalDate.now()) = status(today) == LeaveStatus.OnLeave

data class DashboardStats(
    val onLeave: Int,
    val totalEmployees: Int,
    val returningThisWeek: Int,
    val returnedWithin30: Int,
    val returnedAfter30: Int,
    val noReturn: Int,
    val records: Int,
    val monthly: List<Pair<Int, Int>>, // month number -> returns in that month
    val chartYear: Int,
    val upcoming: List<LeaveRecord>
)

fun buildStats(employees: List<Employee>, leaves: List<LeaveRecord>, today: LocalDate = LocalDate.now()): DashboardStats {
    val statuses = leaves.map { it to it.status(today) }
    val onLeaveList = statuses.filter { it.second == LeaveStatus.OnLeave }.map { it.first }
    val weekEnd = today.plusDays(7)
    val returningThisWeek = onLeaveList.count {
        val r = parseDate(it.returnDate)
        r != null && !r.isBefore(today) && !r.isAfter(weekEnd)
    }
    val returned = statuses.mapNotNull { (it.second as? LeaveStatus.Returned)?.daysAgo }
    // Half-year window containing today, as in the prototype (Jul–Dec).
    val firstMonth = if (today.monthValue <= 6) 1 else 7
    val perMonth = (firstMonth until firstMonth + 6).map { m ->
        m to leaves.count { l ->
            val r = parseDate(l.actualReturnDate) ?: parseDate(l.returnDate)
            r != null && r.year == today.year && r.monthValue == m
        }
    }
    return DashboardStats(
        onLeave = onLeaveList.size,
        totalEmployees = employees.size,
        returningThisWeek = returningThisWeek,
        returnedWithin30 = returned.count { it <= 30 },
        returnedAfter30 = returned.count { it > 30 },
        noReturn = statuses.count { it.second == LeaveStatus.NoReturn },
        records = leaves.size,
        monthly = perMonth,
        chartYear = today.year,
        upcoming = onLeaveList.sortedBy { it.returnDate.ifBlank { "9999" } }
    )
}

fun daysSinceLastReturn(history: List<LeaveRecord>, today: LocalDate = LocalDate.now()): Int? =
    history.mapNotNull { (it.status(today) as? LeaveStatus.Returned)?.daysAgo }.minOrNull()

fun initialOf(name: String): String = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "؟"

fun isTechnician(e: Employee) = e.job.contains("technician", ignoreCase = true)
fun isLifeGuard(e: Employee) = e.job.contains("life", ignoreCase = true)

/* ---------- Shift planning ---------- */

data class Area(val key: String, val label: String, val capacity: Int)

val AREAS = listOf(
    Area("Tirana Main Pool", "الرئيسي", 9),
    Area("Tirana Family Pool", "فاميلي", 5),
    Area("Blue Planet Pool", "بلو بلانت", 10),
    Area("Relax Pool", "ريلاكس", 1),
    Area("Kafkas Pool", "كفكاس", 2),
    Area("Pool Bar", "بول بار", 2)
)
val TECHNICIAN_AREA = Area("Technician", "الفنيون", 4)

fun areaLabel(key: String) = (AREAS + TECHNICIAN_AREA).firstOrNull { it.key.equals(key, true) }?.label ?: key

data class ShiftRange(val index: Int, val start: LocalDate, val end: LocalDate) {
    val label: String get() = "شيفت ${index + 1} · ${start.dayOfMonth}–${end.dayOfMonth}"
}

fun shiftsOf(ym: YearMonth): List<ShiftRange> = listOf(
    ShiftRange(0, ym.atDay(1), ym.atDay(10)),
    ShiftRange(1, ym.atDay(11), ym.atDay(20)),
    ShiftRange(2, ym.atDay(21), ym.atEndOfMonth())
)

/** True if the employee has a leave that overlaps the shift and is not already finished before it starts. */
fun hasLeaveOverlap(code: Int, leaves: List<LeaveRecord>, shift: ShiftRange): Boolean = leaves.any { l ->
    if (l.employeeCode != code) return@any false
    val start = parseDate(l.startDate) ?: return@any false
    val end = parseDate(l.actualReturnDate) ?: parseDate(l.returnDate) ?: return@any false
    !start.isAfter(shift.end) && !end.isBefore(shift.start)
}
