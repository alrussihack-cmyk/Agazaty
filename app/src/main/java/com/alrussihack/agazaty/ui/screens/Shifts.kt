package com.alrussihack.agazaty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.data.ShiftAssignment
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import java.time.YearMonth

@Composable
fun ShiftsScreen(
    assignments: List<ShiftAssignment>,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    shiftIndex: Int,
    onShift: (Int) -> Unit,
    onAssign: () -> Unit,
    onOpenArea: (Area) -> Unit
) {
    val shifts = shiftsOf(month)
    val shift = shifts[shiftIndex]
    val current = assignments.filter { it.shiftStart == shift.start.toString() }
    val technicians = current.filter { it.area.equals(TECHNICIAN_AREA.key, true) }
    val gridAreas = AREAS
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader("توزيع الشيفتات", eyebrow = "الخطة السنوية") { MonthChip(month, onMonth) }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shifts.forEach { s ->
                    Pill(s.label, shiftIndex == s.index, { onShift(s.index) }, Modifier.weight(1f), small = true)
                }
            }
        }
        items(gridAreas.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { area ->
                    AreaCard(area, current.filter { it.area.equals(area.key, true) }, Modifier.weight(1f).fillMaxHeight()) { onOpenArea(area) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (technicians.isNotEmpty()) {
            item { AreaCard(TECHNICIAN_AREA, technicians, Modifier.fillMaxWidth()) { onOpenArea(TECHNICIAN_AREA) } }
        }
        item {
            AgzCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("إجمالي الموزّعين في هذا الشيفت", style = AgzType.Body, modifier = Modifier.weight(1f))
                    Text(current.size.toString(), style = AgzType.Metric.copy(fontSize = 24.sp, lineHeight = 30.sp))
                }
            }
        }
        item { PrimaryButton("+ توزيع موظف على منطقة", onAssign, Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun AreaCard(area: Area, members: List<ShiftAssignment>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AgzCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(area.label, style = AgzType.CardTitle, modifier = Modifier.weight(1f), maxLines = 1)
                Text(members.size.toString(), style = AgzType.Metric.copy(color = Agz.Primary, fontSize = 28.sp, lineHeight = 34.sp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                members.joinToString("، ") { it.employeeName }.ifBlank { "لا يوجد موظفون بعد" },
                style = AgzType.Body.copy(fontSize = 14.sp, lineHeight = 22.sp),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonthChip(month: YearMonth, onMonth: (YearMonth) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val today = YearMonth.now()
    val options = (0 until 24).map { YearMonth.of(today.year, 1).plusMonths(it.toLong()) }
    Box {
        Box(
            Modifier.clip(AgzShape.Pill).background(Agz.Pale).clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) { Text(monthLabel(month), style = AgzType.BodyStrong.copy(color = Agz.PrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Agz.Surface)) {
            options.forEach { ym ->
                DropdownMenuItem(
                    text = { Text(monthLabel(ym), style = AgzType.Input.copy(fontSize = 15.sp, color = if (ym == month) Agz.Primary else Agz.Ink)) },
                    onClick = { onMonth(ym); open = false }
                )
            }
        }
    }
}
