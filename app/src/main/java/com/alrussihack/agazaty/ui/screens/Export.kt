package com.alrussihack.agazaty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.data.ShiftAssignment
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.export.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Composable
private fun ExportOption(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val shape = AgzShape.Card
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(shape)
            .background(if (selected) Agz.Pale else Agz.Surface)
            .border(1.dp, if (selected) Agz.Primary else Agz.Border, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AgzType.CardTitle)
            Text(detail, style = AgzType.Small)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) Agz.Primary else Color.Transparent)
                .border(1.5.dp, if (selected) Agz.Primary else Agz.Muted.copy(alpha = 0.45f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** Export screen: pick shifts / leaves / returns, then save the coloured .xlsx to Downloads or share it. */
@Composable
fun ExportForm(
    employees: List<Employee>,
    leaves: List<LeaveRecord>,
    assignments: List<ShiftAssignment>,
    month: YearMonth,
    initial: Set<ExportKind>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var kinds by remember { mutableStateOf(initial) }
    var allMonths by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    val shiftCount = assignments.count { a -> allMonths || parseDate(a.shiftStart)?.let { YearMonth.from(it) == month } == true }
    val returnedCount = leaves.count { it.status(today) is LeaveStatus.Returned }
    fun toggle(k: ExportKind) { kinds = if (k in kinds) kinds - k else kinds + k; message = null; failed = false }

    fun run(save: Boolean) {
        if (kinds.isEmpty() || busy) return
        busy = true; message = null; failed = false
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) {
                    buildReport(kinds, employees, leaves, assignments, if (allMonths) null else month, today)
                }
                val name = reportFileName(LocalDateTime.now())
                if (save) {
                    withContext(Dispatchers.IO) { saveToDownloads(context, name, bytes) }
                    message = "تم حفظ الملف في مجلد التنزيلات باسم $name"
                } else {
                    val uri = withContext(Dispatchers.IO) { writeShareable(context, name, bytes) }
                    shareXlsx(context, uri, name)
                }
            } catch (e: Exception) {
                failed = true
            } finally {
                busy = false
            }
        }
    }

    FormScaffold("تصدير إلى Excel", onClose) {
        Text(
            "اختر ما تريد تصديره. يُنشأ ملف واحد، وكل اختيار في ورقة مستقلة بألوان التطبيق.",
            style = AgzType.Body, modifier = Modifier.padding(bottom = 16.dp)
        )
        ExportOption("الشيفتات", "توزيع الموظفين على المناطق · $shiftCount موظف", ExportKind.Shifts in kinds) { toggle(ExportKind.Shifts) }
        ExportOption("الإجازات", "كل سجلات الإجازات وحالتها · ${leaves.size} سجل", ExportKind.Leaves in kinds) { toggle(ExportKind.Leaves) }
        ExportOption("العودات", "من عادوا من الإجازة · $returnedCount سجل", ExportKind.Returns in kinds) { toggle(ExportKind.Returns) }

        if (ExportKind.Shifts in kinds) {
            FieldLabel("نطاق الشيفتات", Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(monthLabel(month), !allMonths, { allMonths = false })
                Pill("كل الشهور", allMonths, { allMonths = true })
            }
        }

        if (canSaveToDownloads) {
            PrimaryButton(if (busy) "جارٍ إنشاء الملف…" else "حفظ في التنزيلات", { run(save = true) }, Modifier.padding(top = 24.dp), enabled = kinds.isNotEmpty() && !busy)
            SecondaryButton("مشاركة الملف", { run(save = false) }, Modifier.padding(top = 10.dp), enabled = kinds.isNotEmpty() && !busy)
        } else {
            PrimaryButton(if (busy) "جارٍ إنشاء الملف…" else "مشاركة الملف", { run(save = false) }, Modifier.padding(top = 24.dp), enabled = kinds.isNotEmpty() && !busy)
        }

        if (kinds.isEmpty()) Text("اختر عنصرًا واحدًا على الأقل.", style = AgzType.Small.copy(color = Agz.PrimaryDark), modifier = Modifier.padding(top = 10.dp))
        message?.let { NoticeCard(it, Modifier.padding(top = 16.dp)) }
        if (failed) Text("تعذّر إنشاء الملف. حاول مرة أخرى.", style = AgzType.Small.copy(color = Agz.PrimaryDark), modifier = Modifier.padding(top = 10.dp))
    }
}
