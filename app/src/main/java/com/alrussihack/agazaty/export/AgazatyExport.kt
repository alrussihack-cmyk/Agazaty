package com.alrussihack.agazaty.export

import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.data.ShiftAssignment
import com.alrussihack.agazaty.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ExportKind(val sheetName: String) {
    Shifts("الشيفتات"),
    Leaves("الإجازات"),
    Returns("العودات")
}

/** App identity colours (same tokens as ui/theme/Theme.kt), as RGB hex without alpha. */
private object C {
    const val Ink = "2B1B10"
    const val Muted = "6B5645"
    const val Primary = "C2410C"
    const val PrimaryDark = "9A3412"
    const val Pale = "FFEDD5"
    const val Peach = "FDBA74"
    const val Cream = "FFF8F1"
    const val White = "FFFFFF"
    const val Mint = "CCFBF1"; const val MintInk = "115E59"
    const val Sky = "DBEAFE"; const val SkyInk = "1E40AF"
    const val Stone = "E7E5E4"; const val StoneInk = "44403C"
}

private val titleStyle = CellStyle(bold = true, size = 18, color = C.White, fill = C.PrimaryDark)
private val subtitleStyle = CellStyle(size = 10, color = C.PrimaryDark, fill = C.Pale)
private val headerStyle = CellStyle(bold = true, color = C.White, fill = C.Primary, border = true, align = HAlign.Center, wrap = true)

private fun band(row: Int) = if (row % 2 == 0) C.White else C.Cream
private fun text(row: Int, bold: Boolean = false, color: String = C.Ink) = CellStyle(bold = bold, color = color, fill = band(row), border = true)
private fun centered(row: Int, bold: Boolean = false, color: String = C.Ink, fill: String = band(row)) =
    CellStyle(bold = bold, color = color, fill = fill, border = true, align = HAlign.Center)

private fun s(row: Int, v: String?, bold: Boolean = false, color: String = C.Ink) = Cell(v ?: "", text(row, bold, color))
private fun c(row: Int, v: Any?, bold: Boolean = false, color: String = C.Ink, fill: String = band(row)) = Cell(v, centered(row, bold, color, fill))
private fun d(row: Int, v: LocalDate?) = dateCell(v, centered(row))

/** Builds a sheet with title bar, summary line, frozen coloured header and filter. */
private class Table(name: String, tab: String, private val widths: List<Double>, title: String, subtitle: String, headers: List<String>) {
    val sheet = Sheet(name, tab, widths)
    private var dataRows = 0

    init {
        val n = headers.size
        val t = sheet.addRow(listOf(Cell(title, titleStyle)) + List(n - 1) { Cell(null, titleStyle) }, height = 36.0)
        sheet.merge(t, 0, n - 1)
        val st = sheet.addRow(listOf(Cell(subtitle, subtitleStyle)) + List(n - 1) { Cell(null, subtitleStyle) }, height = 24.0)
        sheet.merge(st, 0, n - 1)
        sheet.addRow(List(n) { Cell(null, CellStyle()) }, height = 8.0)
        val h = sheet.addRow(headers.map { Cell(it, headerStyle) }, height = 30.0)
        sheet.freezeRows = h
        sheet.filterHeaderRow = h
    }

    /** Alternate banding is computed from the data row index. */
    fun add(build: (Int) -> List<Cell>) {
        sheet.addRow(build(dataRows), height = 24.0)
        dataRows++
    }

    fun finish(emptyMessage: String): Sheet {
        if (dataRows == 0) {
            sheet.filterHeaderRow = null
            val n = widths.size
            val style = CellStyle(color = C.Muted, align = HAlign.Center)
            val r = sheet.addRow(listOf(Cell(emptyMessage, style)) + List(n - 1) { Cell(null, style) }, height = 30.0)
            sheet.merge(r, 0, n - 1)
        }
        return sheet
    }
}

private fun statusCells(row: Int, status: LeaveStatus): Cell = when (status) {
    LeaveStatus.OnLeave -> c(row, "في إجازة", true, C.PrimaryDark, C.Pale)
    LeaveStatus.Upcoming -> c(row, "إجازة قادمة", true, C.PrimaryDark, C.Pale)
    LeaveStatus.NoReturn -> c(row, "غير مسجّل", true, C.StoneInk, C.Stone)
    is LeaveStatus.Returned ->
        if (status.daysAgo > 30) c(row, "عاد ${daysAgoText(status.daysAgo)}", true, C.SkyInk, C.Sky)
        else c(row, "عاد ${daysAgoText(status.daysAgo)}", true, C.MintInk, C.Mint)
}

private fun durationDays(l: LeaveRecord): Long? {
    val start = parseDate(l.startDate) ?: return null
    val end = parseDate(l.actualReturnDate) ?: parseDate(l.returnDate) ?: return null
    return ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
}

private fun areaOrder(key: String): Int = (AREAS + TECHNICIAN_AREA).indexOfFirst { it.key.equals(key, true) }.let { if (it < 0) 99 else it }

private fun shiftsSheet(
    employees: List<Employee>, assignments: List<ShiftAssignment>, month: YearMonth?, today: LocalDate
): Sheet {
    val jobByCode = employees.associate { it.code to it.job }
    val rows = assignments
        .filter { a -> month == null || parseDate(a.shiftStart)?.let { YearMonth.from(it) == month } == true }
        .sortedWith(compareBy({ it.shiftStart }, { areaOrder(it.area) }, { it.employeeName.lowercase() }))
    val scope = if (month == null) "كل الشهور" else monthLabel(month)
    val t = Table(
        ExportKind.Shifts.sheetName, C.Primary, listOf(14.0, 17.0, 13.0, 13.0, 16.0, 10.0, 34.0, 18.0),
        "توزيع الشيفتات · $scope",
        "تاريخ التصدير: ${fullDateLabel(today)} · ${rows.size} موظف موزّع",
        listOf("الشهر", "الشيفت", "من", "إلى", "المنطقة", "الكود", "اسم الموظف", "الوظيفة")
    )
    rows.forEach { a ->
        val start = parseDate(a.shiftStart)
        val end = parseDate(a.shiftEnd)
        val ym = start?.let { YearMonth.from(it) }
        val shiftLabel = ym?.let { m -> shiftsOf(m).firstOrNull { it.start == start }?.let { "شيفت ${it.index + 1}" } } ?: "—"
        t.add { i ->
            listOf(
                c(i, ym?.let { monthLabel(it) } ?: "—"),
                c(i, shiftLabel, true, C.PrimaryDark, C.Pale),
                d(i, start), d(i, end),
                c(i, areaLabel(a.area), true, C.PrimaryDark),
                c(i, a.employeeCode),
                s(i, a.employeeName),
                s(i, jobByCode[a.employeeCode] ?: "")
            )
        }
    }
    return t.finish("لا توجد توزيعات شيفتات في هذا النطاق")
}

private fun leavesSheet(leaves: List<LeaveRecord>, today: LocalDate): Sheet {
    val rows = leaves.sortedWith(compareByDescending<LeaveRecord> { it.startDate }.thenByDescending { it.id })
    val onLeave = rows.count { it.status(today) == LeaveStatus.OnLeave }
    val t = Table(
        ExportKind.Leaves.sheetName, C.Peach, listOf(10.0, 34.0, 18.0, 11.0, 14.0, 16.0, 16.0, 12.0, 20.0, 34.0),
        "سجل الإجازات",
        "تاريخ التصدير: ${fullDateLabel(today)} · ${rows.size} سجل · $onLeave في إجازة حاليًا",
        listOf("الكود", "الاسم", "الوظيفة", "النوع", "تاريخ البدء", "العودة المتوقعة", "العودة الفعلية", "المدة (يوم)", "الحالة", "ملاحظات")
    )
    rows.forEach { l ->
        t.add { i ->
            listOf(
                c(i, l.employeeCode),
                s(i, l.employeeName, bold = true),
                s(i, l.job),
                c(i, l.type),
                d(i, parseDate(l.startDate)),
                d(i, parseDate(l.returnDate)),
                d(i, parseDate(l.actualReturnDate)),
                c(i, durationDays(l)),
                statusCells(i, l.status(today)),
                s(i, l.notes, color = C.Muted)
            )
        }
    }
    return t.finish("لا توجد إجازات مسجّلة")
}

private fun returnsSheet(leaves: List<LeaveRecord>, today: LocalDate): Sheet {
    val returned = leaves.mapNotNull { l ->
        val st = l.status(today)
        if (st is LeaveStatus.Returned) Triple(l, st.daysAgo, parseDate(l.actualReturnDate) ?: parseDate(l.returnDate)) else null
    }.sortedWith(compareByDescending<Triple<LeaveRecord, Int, LocalDate?>> { it.third }.thenByDescending { it.first.id })
    val within = returned.count { it.second <= 30 }
    val t = Table(
        ExportKind.Returns.sheetName, C.MintInk, listOf(10.0, 34.0, 18.0, 11.0, 14.0, 14.0, 14.0, 16.0, 20.0),
        "سجل العودات",
        "تاريخ التصدير: ${fullDateLabel(today)} · عادوا خلال 30 يومًا: $within · عادوا بعد 30 يومًا: ${returned.size - within}",
        listOf("الكود", "الاسم", "الوظيفة", "النوع", "تاريخ البدء", "تاريخ العودة", "مدة الإجازة (يوم)", "منذ العودة (يوم)", "الفئة")
    )
    returned.forEach { (l, daysAgo, ret) ->
        t.add { i ->
            listOf(
                c(i, l.employeeCode),
                s(i, l.employeeName, bold = true),
                s(i, l.job),
                c(i, l.type),
                d(i, parseDate(l.startDate)),
                d(i, ret),
                c(i, durationDays(l)),
                c(i, daysAgo),
                if (daysAgo > 30) c(i, "بعد 30 يومًا", true, C.SkyInk, C.Sky) else c(i, "خلال 30 يومًا", true, C.MintInk, C.Mint)
            )
        }
    }
    return t.finish("لا توجد عودات مسجّلة")
}

/** One workbook, one coloured sheet per selected kind (shifts → leaves → returns). shiftMonth = null exports every month. */
fun buildReport(
    kinds: Set<ExportKind>,
    employees: List<Employee>,
    leaves: List<LeaveRecord>,
    assignments: List<ShiftAssignment>,
    shiftMonth: YearMonth?,
    today: LocalDate = LocalDate.now()
): ByteArray {
    val sheets = ExportKind.entries.filter { it in kinds }.map { kind ->
        when (kind) {
            ExportKind.Shifts -> shiftsSheet(employees, assignments, shiftMonth, today)
            ExportKind.Leaves -> leavesSheet(leaves, today)
            ExportKind.Returns -> returnsSheet(leaves, today)
        }
    }
    return buildXlsx(sheets)
}

fun reportFileName(now: LocalDateTime = LocalDateTime.now()): String =
    "Agazaty-${now.toLocalDate()}-${now.hour.toString().padStart(2, '0')}${now.minute.toString().padStart(2, '0')}.xlsx"
