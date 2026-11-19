package com.alrussihack.agazaty.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alrussihack.agazaty.AgazatyViewModel
import com.alrussihack.agazaty.data.Employee
import com.alrussihack.agazaty.data.LeaveRecord
import com.alrussihack.agazaty.domain.*
import com.alrussihack.agazaty.ui.components.AgzBottomBar
import com.alrussihack.agazaty.ui.screens.*
import com.alrussihack.agazaty.ui.theme.Agz
import java.time.LocalDate
import java.time.YearMonth

private sealed interface Overlay {
    data class Leave(val existing: LeaveRecord?, val preset: Employee?) : Overlay
    data object NewEmployee : Overlay
    data class Assign(val area: Area?) : Overlay
    data class AreaInfo(val area: Area) : Overlay
}

private val ADD_LABELS = listOf("إضافة إجازة", "إضافة إجازة", "توزيع موظف على منطقة", "إضافة موظف")

@Composable
fun AgazatyApp(vm: AgazatyViewModel) {
    val employees by vm.employees.collectAsStateWithLifecycle()
    val leaves by vm.leaves.collectAsStateWithLifecycle()
    val assignments by vm.assignments.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val stats = remember(employees, leaves, today) { buildStats(employees, leaves, today) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var profileCode by rememberSaveable { mutableStateOf<Int?>(null) }
    var leaveQuery by rememberSaveable { mutableStateOf("") }
    var leaveFilter by rememberSaveable { mutableIntStateOf(0) }
    var empQuery by rememberSaveable { mutableStateOf("") }
    var empFilter by rememberSaveable { mutableIntStateOf(0) }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var shiftIdx by rememberSaveable { mutableIntStateOf(if (today.dayOfMonth <= 10) 0 else if (today.dayOfMonth <= 20) 1 else 2) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val lastOverlay = remember { mutableStateOf<Overlay?>(null) }
    SideEffect { if (overlay != null) lastOverlay.value = overlay }

    val month = remember(monthKey) { YearMonth.parse(monthKey) }
    val profile = profileCode?.let { c -> employees.firstOrNull { it.code == c } }

    BackHandler(enabled = profile == null && tab != 0 && overlay == null) { tab = 0 }

    Box(Modifier.fillMaxSize().background(Agz.Cream)) {
        if (profile != null) {
            val history = remember(leaves, profile) {
                leaves.filter { it.employeeCode == profile.code }.sortedByDescending { it.startDate.ifBlank { it.returnDate } }
            }
            EmployeeProfileScreen(
                employee = profile, history = history, today = today,
                onBack = { profileCode = null },
                onAddLeave = { overlay = Overlay.Leave(null, profile) },
                onOpenLeave = { overlay = Overlay.Leave(it, null) }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).statusBarsPadding()) {
                    Crossfade(targetState = tab, label = "tab") { t ->
                        when (t) {
                            0 -> DashboardScreen(stats, today, onSearch = { tab = 1 }, onOpenLeave = { overlay = Overlay.Leave(it, null) })
                            1 -> LeavesScreen(leaves, today, leaveQuery, { leaveQuery = it }, leaveFilter, { leaveFilter = it }, onOpen = { overlay = Overlay.Leave(it, null) })
                            2 -> ShiftsScreen(
                                assignments, month, { monthKey = it.toString() }, shiftIdx, { shiftIdx = it },
                                onAssign = { overlay = Overlay.Assign(null) }, onOpenArea = { overlay = Overlay.AreaInfo(it) }
                            )
                            else -> EmployeesScreen(employees, leaves, today, empQuery, { empQuery = it }, empFilter, { empFilter = it }, onOpen = { profileCode = it.code })
                        }
                    }
                }
                AgzBottomBar(
                    selected = tab, onSelect = { tab = it }, addDescription = ADD_LABELS[tab],
                    onAdd = {
                        overlay = when (tab) {
                            2 -> Overlay.Assign(null)
                            3 -> Overlay.NewEmployee
                            else -> Overlay.Leave(null, null)
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = overlay != null,
            enter = slideInVertically(tween(260)) { it / 8 } + fadeIn(tween(220)),
            exit = fadeOut(tween(140))
        ) {
            val o = overlay ?: lastOverlay.value
            Box(Modifier.fillMaxSize().background(Agz.Cream)) {
                val close = { overlay = null }
                when (o) {
                    is Overlay.Leave -> LeaveForm(
                        employees = employees, existing = o.existing, preset = o.preset,
                        onSave = { emp, type, start, exp, act, notes -> vm.saveLeave(o.existing, emp, type, start, exp, act, notes) },
                        onDelete = o.existing?.let { l -> { vm.deleteLeave(l) } },
                        onClose = close
                    )
                    Overlay.NewEmployee -> EmployeeForm(employees.map { it.code }.toSet(), { vm.addEmployee(it) }, close)
                    is Overlay.Assign -> AssignForm(employees, leaves, assignments, month, shiftIdx, o.area, { vm.assign(it) }, close)
                    is Overlay.AreaInfo -> {
                        val shift = shiftsOf(month)[shiftIdx]
                        AreaDetail(
                            area = o.area,
                            members = assignments.filter { it.shiftStart == shift.start.toString() && it.area.equals(o.area.key, true) },
                            shiftLabel = "${shift.label} · ${monthLabel(month)}",
                            onRemove = { vm.unassign(it) },
                            onAdd = { overlay = Overlay.Assign(o.area) },
                            onClose = close
                        )
                    }
                    null -> Unit
                }
            }
        }
    }
}
