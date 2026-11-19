package com.alrussihack.agazaty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alrussihack.agazaty.data.*
import com.alrussihack.agazaty.domain.parseDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

class AgazatyViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AgazatyDb.get(app).dao()

    val employees: StateFlow<List<Employee>> = dao.employees().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val leaves: StateFlow<List<LeaveRecord>> = dao.leaves().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val assignments: StateFlow<List<ShiftAssignment>> = dao.assignments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { viewModelScope.launch { seedIfNeeded() } }

    fun addEmployee(e: Employee) { viewModelScope.launch { dao.insertEmployee(e) } }

    fun saveLeave(existing: LeaveRecord?, employee: Employee, type: String, start: String, expected: String, actual: String, notes: String) {
        viewModelScope.launch {
            val record = (existing ?: LeaveRecord(employeeCode = employee.code, employeeName = employee.name, job = employee.job, startDate = start, returnDate = expected))
                .copy(
                    employeeCode = employee.code, employeeName = employee.name, job = employee.job,
                    startDate = start, returnDate = expected, actualReturnDate = actual, type = type, notes = notes
                )
            if (existing == null) dao.insertLeave(record) else dao.updateLeave(record)
        }
    }

    fun deleteLeave(l: LeaveRecord) { viewModelScope.launch { dao.deleteLeave(l) } }
    fun assign(a: ShiftAssignment) { viewModelScope.launch { dao.insertAssignment(a) } }
    fun unassign(a: ShiftAssignment) { viewModelScope.launch { dao.deleteAssignment(a) } }

    /** First run: import the bundled workbook data. Leaves whose return date has passed are recorded as returned. */
    private suspend fun seedIfNeeded() {
        val data = JSONObject(getApplication<Application>().assets.open("seed.json").bufferedReader().use { it.readText() })
        val emps = data.getJSONArray("employees")
        for (i in 0 until emps.length()) {
            val e = emps.getJSONObject(i)
            val code = e.getInt("code")
            if (dao.employee(code) == null) dao.insertEmployee(Employee(code, e.getString("name"), e.getString("job")))
        }
        if (dao.leaveCount() == 0) {
            val today = LocalDate.now()
            val ls = data.getJSONArray("leaves")
            for (i in 0 until ls.length()) {
                val l = ls.getJSONObject(i)
                val ret = l.optString("returnDate", "")
                val returned = parseDate(ret)?.let { !it.isAfter(today) } == true
                dao.insertLeave(
                    LeaveRecord(
                        employeeCode = l.getInt("code"), employeeName = l.getString("name"), job = l.getString("job"),
                        startDate = l.optString("start", ""), returnDate = ret,
                        actualReturnDate = if (returned) ret else ""
                    )
                )
            }
        }
    }
}
