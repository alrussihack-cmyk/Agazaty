package com.alrussihack.agazaty.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.data.ShiftAssignment
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/* ---------- shared pieces ---------- */

/** Fires [action] on every touch of the field without consuming it, so the list reopens even when the field is already focused. */
private fun Modifier.onTouch(action: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        action()
    }
}

@Composable
private fun SelectMark(selected: Boolean) {
    Box(
        Modifier.size(24.dp).clip(CircleShape)
            .background(if (selected) Agz.Primary else Color.Transparent)
            .border(1.5.dp, if (selected) Agz.Primary else Agz.Muted.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** Scrollable list of every employee (filtered by the query). Rows with a reason are shown dimmed and cannot be picked. */
@Composable
private fun EmployeeListCard(
    employees: List<Employee>,
    query: String,
    isSelected: (Employee) -> Boolean,
    disabledReason: (Employee) -> String?,
    multi: Boolean,
    onTap: (Employee) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val q = query.trim()
    val shown = remember(employees, q) {
        if (q.isEmpty()) employees else employees.filter { it.name.contains(q, true) || it.code.toString().startsWith(q) }
    }
    AgzCard(modifier.fillMaxWidth().padding(top = 6.dp)) {
        Column {
            if (header != null) {
                header()
                Box(Modifier.fillMaxWidth().height(1.dp).background(Agz.Border))
            }
            if (shown.isEmpty()) {
                Text("لا يوجد موظف بهذا الاسم أو الكود", style = AgzType.Body, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(shown, key = { it.code }) { e ->
                        val selected = isSelected(e)
                        val reason = disabledReason(e)
                        val locked = reason != null && !selected
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (selected) Agz.Pale else Color.Transparent)
                                .clickable(enabled = !locked) { onTap(e) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .alpha(if (locked) 0.5f else 1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(initialOf(e.name), size = 34.dp, fontSize = 17.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = AgzType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (reason != null) Text(reason, style = AgzType.Small.copy(color = Agz.PrimaryDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                else Text("${e.job} · ${e.code}", style = AgzType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            if (multi) SelectMark(selected)
                            else if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Agz.Primary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Single-choice picker: tapping the field lists every employee; typing narrows the list by name or code. */
@Composable
fun EmployeePicker(employees: List<Employee>, selected: Employee?, onSelect: (Employee?) -> Unit) {
    var query by remember { mutableStateOf(selected?.name ?: "") }
    var open by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    BackHandler(enabled = open) { open = false; focus.clearFocus() }
    AgzTextField(
        value = query,
        onChange = { text ->
            query = text
            open = true
            if (selected != null && text != selected.name) onSelect(null)
        },
        placeholder = "ابحث بالاسم أو الكود",
        modifier = Modifier.onTouch { open = true }.onFocusChanged { if (it.isFocused) open = true }
    )
    if (open) {
        val filter = if (selected != null && query == selected.name) "" else query
        EmployeeListCard(
            employees = employees, query = filter,
            isSelected = { it.code == selected?.code },
            disabledReason = { null }, multi = false,
            onTap = { e -> query = e.name; onSelect(e); open = false; focus.clearFocus() }
        )
    }
}

@Composable
private fun SelectedChip(name: String, problem: Boolean, onRemove: () -> Unit) {
    Row(
        Modifier.heightIn(min = 36.dp).clip(AgzShape.Pill).background(Agz.Pale)
            .border(1.dp, if (problem) Agz.Primary else Color.Transparent, AgzShape.Pill)
            .clickable(role = Role.Button, onClick = onRemove)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = AgzType.BodyStrong.copy(color = Agz.PrimaryDark, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.Close, contentDescription = "إزالة $name", tint = Agz.PrimaryDark, modifier = Modifier.size(16.dp))
    }
}

/**
 * Multi-choice picker: tapping the field lists every employee with a checkbox; the list stays open so several
 * names can be ticked in one go. Chosen names are shown as removable chips below.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiEmployeePicker(
    employees: List<Employee>,
    selected: List<Employee>,
    reasonFor: (Employee) -> String?,
    onToggle: (Employee) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val chosen = remember(selected) { selected.map { it.code }.toSet() }
    BackHandler(enabled = open) { open = false; focus.clearFocus() }
    AgzTextField(
        value = query,
        onChange = { query = it; open = true },
        placeholder = "ابحث بالاسم أو الكود",
        modifier = Modifier.onTouch { open = true }.onFocusChanged { if (it.isFocused) open = true }
    )
    if (open) {
        EmployeeListCard(
            employees = employees, query = query,
            isSelected = { it.code in chosen },
            disabledReason = reasonFor, multi = true, onTap = onToggle,
            header = {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (selected.isEmpty()) "اختر موظفًا أو أكثر" else "تم اختيار ${selected.size}",
                        style = AgzType.BodyStrong, modifier = Modifier.weight(1f)
                    )
                    TextAction("تم", { open = false; focus.clearFocus() })
                }
            }
        )
    }
    if (selected.isNotEmpty()) {
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.forEach { e -> SelectedChip(e.name, reasonFor(e) != null, { onToggle(e) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initial: String, onPick: (String) -> Unit, onClear: (() -> Unit)?, onDismiss: () -> Unit) {
    val start = (parseDate(initial) ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = start)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                onDismiss()
            }) { Text("تأكيد", color = Agz.Primary) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = { onClear(); onDismiss() }) { Text("مسح", color = Agz.Muted) }
                TextButton(onClick = onDismiss) { Text("إلغاء", color = Agz.Muted) }
            }
        },
        colors = DatePickerDefaults.colors(containerColor = Agz.Surface)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = Agz.Surface,
                selectedDayContainerColor = Agz.Primary,
                todayContentColor = Agz.Primary,
                todayDateBorderColor = Agz.Primary
            )
        )
    }
}

@Composable
private fun DateField(label: String, value: String, placeholder: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        FieldLabel(label)
        PickerField(fmtFull(value), placeholder, { open = true })
    }
    if (open) DatePickerSheet(value, onPick = onChange, onClear = if (clearable && value.isNotEmpty()) ({ onChange("") }) else null, onDismiss = { open = false })
}

@Composable
private fun ErrorText(text: String) =
    Text(text, style = AgzType.Small.copy(color = Agz.PrimaryDark, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 6.dp))

/* ---------- add / edit leave ---------- */

@Composable
fun LeaveForm(
    employees: List<Employee>,
    existing: LeaveRecord?,
    preset: Employee?,
    onSave: (Employee, String, String, String, String, String) -> Unit, // employee, type, start, expected, actual, notes
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    val initialEmployee = preset ?: existing?.let { l -> employees.firstOrNull { it.code == l.employeeCode } }
    var employee by remember { mutableStateOf(initialEmployee) }
    var type by remember { mutableStateOf(existing?.type ?: LEAVE_TYPES.first()) }
    var start by remember { mutableStateOf(existing?.startDate ?: LocalDate.now().toString()) }
    var expected by remember { mutableStateOf(existing?.returnDate ?: LocalDate.now().plusDays(14).toString()) }
    var actual by remember { mutableStateOf(existing?.actualReturnDate ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val s = parseDate(start); val e = parseDate(expected); val a = parseDate(actual)
    val expectedBeforeStart = s != null && e != null && e.isBefore(s)
    val actualBeforeStart = s != null && a != null && a.isBefore(s)
    val valid = employee != null && s != null && e != null && !expectedBeforeStart && !actualBeforeStart

    val preview = LeaveRecord(employeeCode = employee?.code ?: 0, employeeName = "", job = "", startDate = start, returnDate = expected, actualReturnDate = actual).status()
    val statusText = when (preview) {
        LeaveStatus.OnLeave -> "في إجازة حاليًا"
        LeaveStatus.Upcoming -> "إجازة قادمة"
        LeaveStatus.NoReturn -> "غير مسجّلة"
        is LeaveStatus.Returned -> "عاد ${daysAgoText(preview.daysAgo)}"
    }

    FormScaffold(if (existing == null) "إضافة إجازة" else "تعديل إجازة", onClose) {
        FieldLabel("اسم الموظف")
        EmployeePicker(employees, employee) { employee = it }
        employee?.let { AutofillCard(initialOf(it.name), it.code, it.job, Modifier.padding(top = 10.dp)) }

        FieldLabel("نوع الإجازة", Modifier.padding(top = 20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LEAVE_TYPES.forEach { t -> Pill(t, type == t, { type = t }) } }

        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("تاريخ البدء", start, "اختر التاريخ", { start = it }, Modifier.weight(1f))
            DateField("العودة المتوقعة", expected, "اختر التاريخ", { expected = it }, Modifier.weight(1f))
        }
        if (expectedBeforeStart) ErrorText("تاريخ العودة قبل تاريخ البدء")

        DateField("تاريخ العودة الفعلي (اختياري)", actual, "يُضاف عند الرجوع", { actual = it }, Modifier.padding(top = 20.dp), clearable = true)
        if (actualBeforeStart) ErrorText("تاريخ العودة الفعلي قبل تاريخ البدء")

        FieldLabel("ملاحظات", Modifier.padding(top = 20.dp))
        AgzTextField(notes, { notes = it }, singleLine = false)

        AgzCard(Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text(
                buildAnnotatedString {
                    append("الحالة تُحسب تلقائيًا: ")
                    withStyle(SpanStyle(color = Agz.PrimaryDark, fontWeight = FontWeight.Bold)) { append(statusText) }
                },
                style = AgzType.Body, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

        PrimaryButton("حفظ الإجازة", {
            employee?.let { onSave(it, type, start, expected, actual, notes.trim()); onClose() }
        }, Modifier.padding(top = 20.dp), enabled = valid)
        if (onDelete != null) {
            TextAction("حذف الإجازة", { confirmDelete = true }, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp), color = Agz.PrimaryDark)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف هذه الإجازة؟", style = AgzType.Section) },
            text = { Text("لا يمكن التراجع عن الحذف.", style = AgzType.Body) },
            confirmButton = { TextButton({ onDelete?.invoke(); onClose() }) { Text("حذف", color = Agz.Primary) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("إلغاء", color = Agz.Muted) } },
            containerColor = Agz.Surface
        )
    }
}

/* ---------- add employee ---------- */

private val JOB_SUGGESTIONS = listOf("Life Guard", "Pool Technician", "Tennis Coach", "Gym Coach")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeForm(existingCodes: Set<Int>, onSave: (Employee) -> Unit, onClose: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var job by remember { mutableStateOf(JOB_SUGGESTIONS.first()) }
    val c = code.trim().toIntOrNull()
    val duplicate = c != null && c in existingCodes
    FormScaffold("إضافة موظف", onClose) {
        FieldLabel("الكود")
        AgzTextField(code, { code = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number, placeholder = "مثال: 4198")
        if (duplicate) ErrorText("هذا الكود مستخدم لموظف آخر")
        FieldLabel("الاسم", Modifier.padding(top = 20.dp))
        AgzTextField(name, { name = it })
        FieldLabel("الوظيفة", Modifier.padding(top = 20.dp))
        AgzTextField(job, { job = it })
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JOB_SUGGESTIONS.forEach { Pill(it, job == it, { job = it }) }
        }
        PrimaryButton("حفظ الموظف", {
            c?.let { onSave(Employee(it, name.trim(), job.trim())); onClose() }
        }, Modifier.padding(top = 24.dp), enabled = c != null && !duplicate && name.isNotBlank() && job.isNotBlank())
    }
}

/* ---------- assign employees to an area ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignForm(
    employees: List<Employee>,
    leaves: List<LeaveRecord>,
    assignments: List<ShiftAssignment>,
    month: YearMonth,
    initialShift: Int,
    initialArea: Area?,
    onSave: (List<ShiftAssignment>) -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var area by remember { mutableStateOf(initialArea ?: AREAS.first()) }
    var shiftIdx by remember { mutableIntStateOf(initialShift) }
    val shifts = shiftsOf(month)
    val shift = shifts[shiftIdx]
    val inShift = assignments.filter { it.shiftStart == shift.start.toString() }
    val takenCodes = inShift.map { it.employeeCode }.toSet()
    val remaining = area.capacity - inShift.count { it.area.equals(area.key, true) }

    /** Why an employee cannot be placed in this shift, or null when free. */
    fun baseReason(e: Employee): String? = when {
        hasLeaveOverlap(e.code, leaves, shift) -> "لديه إجازة تتداخل مع هذا الشيفت"
        e.code in takenCodes -> "موزّع بالفعل في هذا الشيفت"
        else -> null
    }

    val problems = selected.filter { baseReason(it) != null }
    val overCapacity = selected.size > remaining
    val valid = selected.isNotEmpty() && problems.isEmpty() && !overCapacity

    FormScaffold("توزيع موظف على منطقة", onClose) {
        FieldLabel("اسم الموظف")
        MultiEmployeePicker(
            employees = employees, selected = selected,
            reasonFor = { e ->
                baseReason(e) ?: if (selected.none { it.code == e.code } && selected.size >= remaining) "اكتمل العدد المطلوب في ${area.label}" else null
            },
            onToggle = { e -> selected = if (selected.any { it.code == e.code }) selected.filterNot { it.code == e.code } else selected + e }
        )

        FieldLabel("المنطقة", Modifier.padding(top = 20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (AREAS + TECHNICIAN_AREA).forEach { a -> Pill(a.label, area == a, { area = a }) }
        }
        Text("المتاح في ${area.label}: ${remaining.coerceAtLeast(0)} من ${area.capacity}", style = AgzType.Small, modifier = Modifier.padding(top = 8.dp))

        FieldLabel("الشيفت · ${monthLabel(month)}", Modifier.padding(top = 20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shifts.forEach { s -> Pill(s.label, shiftIdx == s.index, { shiftIdx = s.index }, Modifier.weight(1f), small = true) }
        }

        problems.forEach { ErrorText("${it.name}: ${baseReason(it)}") }
        if (overCapacity) ErrorText("العدد المختار (${selected.size}) يتجاوز المتاح في ${area.label} (${remaining.coerceAtLeast(0)})")

        PrimaryButton(
            if (selected.size > 1) "حفظ التوزيع (${selected.size})" else "حفظ التوزيع",
            {
                onSave(selected.map {
                    ShiftAssignment(shiftStart = shift.start.toString(), shiftEnd = shift.end.toString(), area = area.key, employeeCode = it.code, employeeName = it.name)
                })
                onClose()
            },
            Modifier.padding(top = 24.dp), enabled = valid
        )
    }
}

/* ---------- area detail: members + remove ---------- */

@Composable
fun AreaDetail(
    area: Area,
    members: List<ShiftAssignment>,
    shiftLabel: String,
    onRemove: (ShiftAssignment) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit
) {
    FormScaffold(area.label, onClose) {
        Text("$shiftLabel · ${members.size} من ${area.capacity}", style = AgzType.Body, modifier = Modifier.padding(bottom = 12.dp))
        if (members.isEmpty()) {
            AgzCard(Modifier.fillMaxWidth()) { Text("لا يوجد موظفون موزّعون على هذه المنطقة في هذا الشيفت.", style = AgzType.Body, modifier = Modifier.padding(18.dp)) }
        }
        members.forEach { m ->
            AgzCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(initialOf(m.employeeName))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.employeeName, style = AgzType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("الكود ${m.employeeCode}", style = AgzType.Small)
                    }
                    TextAction("إزالة", { onRemove(m) }, color = Agz.PrimaryDark)
                }
            }
        }
        PrimaryButton("+ توزيع موظف على هذه المنطقة", onAdd, Modifier.padding(top = 12.dp))
    }
}
