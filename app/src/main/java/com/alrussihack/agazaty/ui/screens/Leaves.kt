package com.alrussihack.agazaty.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import java.time.LocalDate

/** One row used by the log, the dashboard "returning soon" list and the employee profile history. */
@Composable
fun LeaveRow(
    title: String,
    subtitle: String,
    status: LeaveStatus,
    initial: String?,
    modifier: Modifier = Modifier,
    longBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    AgzCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (initial != null) {
                Avatar(initial)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = AgzType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = AgzType.Small.copy(color = Agz.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            LeaveBadge(status, longReturn = longBadge)
        }
    }
}

/** "25/10 ← 10/11 · سنوية" — start → return, then the type. */
fun leaveSubtitle(l: LeaveRecord, withYear: Boolean = false): String {
    val start = parseDate(l.startDate)
    val ret = parseDate(l.actualReturnDate) ?: parseDate(l.returnDate)
    val retText = ret?.let { if (withYear) fmtFull(it) else fmtDayMonth(it) }
    return when {
        start != null && retText != null -> "${fmtDayMonth(start)} ← $retText"
        start != null -> "من ${fmtDayMonth(start)}"
        retText != null -> "تاريخ العودة $retText"
        else -> "سجل بلا تاريخ عودة"
    }.let { if (start == null && retText == null) it else "$it · ${l.type}" }
}

val LEAVE_FILTERS = listOf("الكل", "في إجازة", "عادوا")

@Composable
fun LeavesScreen(
    leaves: List<LeaveRecord>,
    today: LocalDate,
    query: String,
    onQuery: (String) -> Unit,
    filter: Int,
    onFilter: (Int) -> Unit,
    onOpen: (LeaveRecord) -> Unit
) {
    val shown = leaves.filter { l ->
        val q = query.trim()
        val matches = q.isEmpty() || l.employeeName.contains(q, true) || l.employeeCode.toString().contains(q) || l.job.contains(q, true)
        val st = l.status(today)
        matches && when (filter) {
            1 -> st == LeaveStatus.OnLeave || st == LeaveStatus.Upcoming
            2 -> st is LeaveStatus.Returned
            else -> true
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader("سجل الإجازات") { Text("${leaves.size} سجل", style = AgzType.Body) }
        }
        item { SearchField(query, onQuery, "ابحث بالاسم أو الكود") }
        item {
            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LEAVE_FILTERS.forEachIndexed { i, label -> Pill(label, filter == i, { onFilter(i) }) }
            }
        }
        if (shown.isEmpty()) {
            item {
                AgzCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (leaves.isEmpty()) "لا توجد إجازات بعد. اضغط ＋ لإضافة أول إجازة." else "لا توجد سجلات مطابقة للبحث.",
                        style = AgzType.Body, modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }
        items(shown, key = { it.id }) { l ->
            LeaveRow(
                title = l.employeeName,
                subtitle = leaveSubtitle(l),
                status = l.status(today),
                initial = initialOf(l.employeeName),
                onClick = { onOpen(l) }
            )
        }
    }
}
