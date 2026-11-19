package com.alrussihack.agazaty.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import java.time.LocalDate

@Composable
fun EmployeeProfileScreen(
    employee: Employee,
    history: List<LeaveRecord>,
    today: LocalDate,
    onBack: () -> Unit,
    onAddLeave: () -> Unit,
    onOpenLeave: (LeaveRecord) -> Unit
) {
    BackHandler(onBack = onBack)
    val sinceReturn = daysSinceLastReturn(history, today)
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                Text("ملف الموظف", style = AgzType.Heading.copy(fontSize = 22.sp), modifier = Modifier.align(Alignment.Center))
                BackChevron(onBack, Modifier.align(Alignment.CenterStart))
            }
        }
        item {
            AgzCard(Modifier.fillMaxWidth(), shape = AgzShape.Hero) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(initialOf(employee.name), size = 64.dp, fontSize = 32.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(employee.name, style = AgzType.Heading.copy(fontSize = 25.sp), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                    Text("الكود ${employee.code} · ${employee.job}", style = AgzType.Body.copy(fontSize = 14.sp), textAlign = TextAlign.Center)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileMetric(history.size.toString(), "إجازات", Modifier.weight(1f))
                ProfileMetric(sinceReturn?.toString() ?: "—", "يومًا منذ العودة", Modifier.weight(1f))
            }
        }
        item { SectionTitle("تاريخ الإجازات", Modifier.padding(top = 14.dp, bottom = 2.dp)) }
        if (history.isEmpty()) {
            item { AgzCard(Modifier.fillMaxWidth()) { Text("لا توجد إجازات مسجّلة لهذا الموظف بعد.", style = AgzType.Body, modifier = Modifier.padding(18.dp)) } }
        }
        items(history, key = { it.id }) { l ->
            val sub = parseDate(l.startDate)?.let { s ->
                val r = parseDate(l.actualReturnDate) ?: parseDate(l.returnDate)
                if (r != null) "${fmtDayMonth(s)} ← ${fmtFull(r)}" else "من ${fmtFull(s)}"
            } ?: (parseDate(l.actualReturnDate) ?: parseDate(l.returnDate))?.let { "تاريخ العودة ${fmtFull(it)}" } ?: "بلا تواريخ"
            LeaveRow(title = l.type, subtitle = sub, status = l.status(today), initial = null, longBadge = true, onClick = { onOpenLeave(l) })
        }
        item { Spacer(Modifier.height(4.dp)); PrimaryButton("إضافة إجازة لهذا الموظف", onAddLeave) }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    AgzCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = AgzType.Metric.copy(color = Agz.Primary, fontSize = 30.sp))
            Text(label, style = AgzType.Body.copy(fontSize = 14.sp))
        }
    }
}
