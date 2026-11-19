package com.alrussihack.agazaty.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import java.time.LocalDate

@Composable
fun EmployeesScreen(
    employees: List<Employee>,
    leaves: List<LeaveRecord>,
    today: LocalDate,
    query: String,
    onQuery: (String) -> Unit,
    filter: Int,
    onFilter: (Int) -> Unit,
    onOpen: (Employee) -> Unit
) {
    val onLeaveCodes = leaves.filter { it.isOnLeave(today) }.map { it.employeeCode }.toSet()
    val filters = listOf(
        "الكل ${employees.size}",
        "Life Guard ${employees.count(::isLifeGuard)}",
        "فنيو الحمامات ${employees.count(::isTechnician)}"
    )
    val shown = employees.filter { e ->
        val q = query.trim()
        (q.isEmpty() || e.name.contains(q, true) || e.code.toString().contains(q) || e.job.contains(q, true)) &&
            when (filter) { 1 -> isLifeGuard(e); 2 -> isTechnician(e); else -> true }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ScreenHeader("الموظفون") { Text("${employees.size} موظفًا", style = AgzType.Body) } }
        item { SearchField(query, onQuery, "ابحث بالاسم أو الكود", showIcon = false) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEachIndexed { i, label -> Pill(label, filter == i, { onFilter(i) }) }
            }
        }
        if (shown.isEmpty()) {
            item { AgzCard(Modifier.fillMaxWidth()) { Text("لا يوجد موظف مطابق للبحث.", style = AgzType.Body, modifier = Modifier.padding(18.dp)) } }
        }
        items(shown, key = { it.code }) { e ->
            AgzCard(Modifier.fillMaxWidth(), onClick = { onOpen(e) }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(initialOf(e.name))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = AgzType.CardTitle.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${e.job} · ${e.code}", style = AgzType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (e.code in onLeaveCodes) Badge("في إجازة", Agz.Pale, Agz.PrimaryDark) else Badge("نشط", Agz.Mint, Agz.MintInk)
                }
            }
        }
    }
}
