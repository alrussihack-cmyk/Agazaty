package com.alrussihack.agazaty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.*
import com.alrussihack.agazaty.ui.theme.*
import java.time.LocalDate

@Composable
fun DashboardScreen(stats: DashboardStats, today: LocalDate, onSearch: () -> Unit, onOpenLeave: (LeaveRecord) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader(title = "لوحة الإجازات", eyebrow = fullDateLabel(today), centerTrailing = false) {
                IconAction(Icons.Outlined.Search, "بحث في الإجازات", onSearch)
            }
        }
        item { HeroCard(stats) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("عادوا خلال 30 يومًا", stats.returnedWithin30, Agz.MintInk, Agz.MintInk, Modifier.weight(1f))
                MetricCard("عادوا بعد 30 يومًا", stats.returnedAfter30, Agz.SkyInk, Agz.SkyInk, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("سجلات الإجازات", stats.records, Agz.Primary, Agz.PrimaryDark, Modifier.weight(1f))
                MetricCard("عودة غير مسجّلة", stats.noReturn, Agz.StoneDot, Agz.StoneInk, Modifier.weight(1f))
            }
        }
        item { MonthlyReturnsCard(stats) }
        item { SectionTitle("يعودون قريبًا", Modifier.padding(top = 12.dp, bottom = 2.dp)) }
        if (stats.upcoming.isEmpty()) {
            item {
                AgzCard(Modifier.fillMaxWidth()) {
                    Text("لا توجد إجازات جارية الآن", style = AgzType.Body, modifier = Modifier.padding(18.dp))
                }
            }
        } else {
            items(stats.upcoming.take(5), key = { it.id }) { l ->
                val ret = fmtDayMonth(l.returnDate)
                LeaveRow(
                    title = l.employeeName,
                    subtitle = if (ret.isEmpty()) l.job else "العودة $ret — ${l.job}",
                    status = l.status(today),
                    initial = initialOf(l.employeeName),
                    onClick = { onOpenLeave(l) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard(stats: DashboardStats) {
    Row(
        Modifier.fillMaxWidth().clip(AgzShape.Hero).background(Agz.Primary).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("في إجازة حاليًا", style = AgzType.Body.copy(color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
            Text(stats.onLeave.toString(), style = AgzType.Display.copy(color = Color.White))
            Text("من إجمالي ${stats.totalEmployees} موظفًا", style = AgzType.Body.copy(color = Color.White))
        }
        if (stats.returningThisWeek > 0) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Agz.OnPrimaryChip).padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${stats.returningThisWeek} يعودون\nهذا الأسبوع",
                    style = AgzType.BodyStrong.copy(color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: Int, dot: Color, valueColor: Color, modifier: Modifier = Modifier) {
    AgzCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(8.dp))
                Text(label, style = AgzType.Body.copy(fontSize = 14.sp), maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Text(value.toString(), style = AgzType.Metric.copy(color = valueColor))
        }
    }
}

@Composable
private fun MonthlyReturnsCard(stats: DashboardStats) {
    val max = stats.monthly.maxOfOrNull { it.second } ?: 0
    val peakMonth = if (max > 0) stats.monthly.first { it.second == max }.first else -1
    AgzCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("العودة الشهرية", style = AgzType.CardTitle.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
                Text(stats.chartYear.toString(), style = AgzType.Small)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.Bottom) {
                stats.monthly.forEach { (month, count) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        val h = if (max == 0 || count == 0) 8.dp else (14 + 54f * count / max).dp
                        Box(
                            Modifier.width(30.dp).height(h)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(if (month == peakMonth) Agz.Primary else Agz.Peach)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(monthName(month), style = AgzType.Small.copy(fontSize = 12.sp), maxLines = 1)
                    }
                }
            }
        }
    }
}
