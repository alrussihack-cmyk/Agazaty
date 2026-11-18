package com.alrussihack.agazaty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.alrussihack.agazaty.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.json.JSONObject

private val Cream = Color(0xFFFFF8F1)
private val Orange = Color(0xFFC83D08)
private val Ink = Color(0xFF302016)
private val Muted = Color(0xFF795F4E)
private val Border = Color(0xFFF3E1D2)
private val Pale = Color(0xFFFFEED9)
private val Mint = Color(0xFFCCF8EF)
private val MintInk = Color(0xFF11665F)
private val BluePale = Color(0xFFDCEAFF)
private val BlueInk = Color(0xFF244DBB)
private val SoftGrey = Color(0xFFE9E7E5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AgazatyDb.get(this)
        lifecycleScope.launch { seedDatabase(db) }
        setContent {
            val employees by db.dao().employees().collectAsStateWithLifecycle(emptyList())
            val leaves by db.dao().leaves().collectAsStateWithLifecycle(emptyList())
            val assignments by db.dao().assignments().collectAsStateWithLifecycle(emptyList())
            AgazatyUI(
                employees, leaves, assignments,
                { e -> lifecycleScope.launch { db.dao().insertEmployee(e) } },
                { code, start, end, notes -> lifecycleScope.launch {
                    db.dao().employee(code)?.let {
                        db.dao().insertLeave(LeaveRecord(employeeCode=code, employeeName=it.name, job=it.job, startDate=start, returnDate=end, notes=notes))
                    }
                } },
                { a -> lifecycleScope.launch { db.dao().insertAssignment(a) } },
                { a -> lifecycleScope.launch { db.dao().deleteAssignment(a) } }
            )
        }
    }

    private suspend fun seedDatabase(db: AgazatyDb) {
        val data = JSONObject(assets.open("seed.json").bufferedReader().use { it.readText() })
        val employees = data.getJSONArray("employees")
        for (i in 0 until employees.length()) {
            val e = employees.getJSONObject(i)
            val code = e.getInt("code")
            if (db.dao().employee(code) == null) db.dao().insertEmployee(Employee(code, e.getString("name"), e.getString("job")))
        }
        if (db.dao().leaveCount() == 0) {
            val leaves = data.getJSONArray("leaves")
            for (i in 0 until leaves.length()) {
                val l = leaves.getJSONObject(i)
                db.dao().insertLeave(LeaveRecord(employeeCode=l.getInt("code"), employeeName=l.getString("name"), job=l.getString("job"), startDate=l.getString("start"), returnDate=l.getString("returnDate")))
            }
        }
    }
}

@Composable
fun AgazatyUI(
    es: List<Employee>, ls: List<LeaveRecord>, assignments: List<ShiftAssignment>,
    addE: (Employee) -> Unit, addL: (Int, String, String, String) -> Unit,
    addA: (ShiftAssignment) -> Unit, removeA: (ShiftAssignment) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var employeeFilter by remember { mutableStateOf("الكل") }
    var leaveFilter by remember { mutableStateOf("الكل") }
    var addEmp by remember { mutableStateOf(false) }
    var addLeave by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary=Orange, background=Cream, surface=Color.White, onSurface=Ink)) {
            Scaffold(
                containerColor = Cream,
                bottomBar = {
                    NavigationBar(containerColor=Color.White, tonalElevation=0.dp, modifier=Modifier.height(78.dp)) {
                        NavigationBarItem(selected=tab==0, onClick={tab=0;selectedEmployee=null}, icon={Icon(Icons.Filled.Home,null)}, label={Text("الرئيسية",fontSize=11.sp)}, colors=navColors())
                        NavigationBarItem(selected=tab==1, onClick={tab=1;selectedEmployee=null}, icon={Icon(Icons.Filled.CalendarMonth,null)}, label={Text("الإجازات",fontSize=11.sp)}, colors=navColors())
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment=Alignment.TopCenter) {
                            FloatingActionButton(
                                onClick={ if(tab==3) addEmp=true else addLeave=true },
                                modifier=Modifier.offset(y=(-18).dp).size(62.dp),
                                shape=CircleShape, containerColor=Orange, contentColor=Color.White,
                                elevation=FloatingActionButtonDefaults.elevation(8.dp)
                            ) { Icon(Icons.Filled.Add, contentDescription="إضافة", modifier=Modifier.size(32.dp)) }
                        }
                        NavigationBarItem(selected=tab==2, onClick={tab=2;selectedEmployee=null}, icon={Icon(Icons.Filled.GridView,null)}, label={Text("الشيفتات",fontSize=11.sp)}, colors=navColors())
                        NavigationBarItem(selected=tab==3, onClick={tab=3;selectedEmployee=null}, icon={Icon(Icons.Filled.People,null)}, label={Text("الموظفون",fontSize=11.sp)}, colors=navColors())
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=18.dp)) {
                    when {
                        selectedEmployee != null -> EmployeeProfile(selectedEmployee!!, ls.filter { it.employeeCode==selectedEmployee!!.code }, { selectedEmployee=null }, { addLeave=true })
                        tab == 0 -> Dashboard(es,ls)
                        tab == 1 -> {
                            ScreenHeader("سجل الإجازات", "${ls.size} سجل")
                            SearchBox(query,{query=it},"ابحث بالاسم أو الكود")
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.padding(vertical=12.dp)) {
                                listOf("الكل","في إجازة","عادوا").forEach { FilterPill(it,leaveFilter==it){leaveFilter=it} }
                            }
                            LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=18.dp)) {
                                items(ls.filter { l ->
                                    (l.employeeName.contains(query,true)||l.employeeCode.toString().contains(query)) &&
                                    (leaveFilter=="الكل" || (leaveFilter=="في إجازة" && isOnLeave(l)) || (leaveFilter=="عادوا" && !isOnLeave(l)))
                                }) { LeaveCard(it) }
                            }
                        }
                        tab == 2 -> {
                            ScreenHeader("توزيع الشيفتات","الخطة الشهرية")
                            ShiftPlanner(es,ls,assignments,addA,removeA)
                        }
                        else -> {
                            ScreenHeader("الموظفون","${es.size} موظفاً")
                            SearchBox(query,{query=it},"ابحث بالاسم أو الكود")
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=12.dp)) {
                                listOf("الكل","Life Guard","فني الحمامات").forEach { FilterPill(it,employeeFilter==it){employeeFilter=it} }
                            }
                            LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                                items(es.filter { e ->
                                    (e.name.contains(query,true)||e.code.toString().contains(query)||e.job.contains(query,true)) &&
                                    (employeeFilter=="الكل" || (employeeFilter=="Life Guard" && e.job.contains("life",true)) || (employeeFilter=="فني الحمامات" && (e.job.contains("technician",true)||e.job.contains("pool",true))))
                                }) { EmployeeCard(it,ls){selectedEmployee=it} }
                            }
                        }
                    }
                }
            }
            if(addEmp) EmployeeForm({addE(it);addEmp=false},{addEmp=false})
            if(addLeave) LeaveForm(es,{c,s,r,n->addL(c,s,r,n);addLeave=false},{addLeave=false})
        }
    }
}

@Composable private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor=Orange, selectedTextColor=Orange, indicatorColor=Color.Transparent,
    unselectedIconColor=Muted, unselectedTextColor=Muted
)

@Composable private fun ScreenHeader(title:String, meta:String) {
    Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(title,fontSize=28.sp,fontWeight=FontWeight.ExtraBold,color=Ink)
        Text(meta,fontSize=14.sp,color=Muted)
    }
}

@Composable private fun SearchBox(value:String,onChange:(String)->Unit,hint:String) {
    OutlinedTextField(
        value=value,onValueChange=onChange,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),
        placeholder={Text(hint,color=Muted,fontSize=16.sp)},singleLine=true,
        trailingIcon={Icon(Icons.Filled.Search,null,tint=Muted)},
        shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(
            focusedBorderColor=Orange,unfocusedBorderColor=Border,focusedContainerColor=Color.White,unfocusedContainerColor=Color.White
        )
    )
}

@Composable private fun FilterPill(label:String,selected:Boolean,onClick:()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(30.dp),color=if(selected) Orange else Color.White,
        border=if(selected) null else BorderStroke(1.dp,Border)) {
        Text(label,Modifier.padding(horizontal=15.dp,vertical=9.dp),fontSize=14.sp,color=if(selected) Color.White else Muted,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable private fun Dashboard(es:List<Employee>,ls:List<LeaveRecord>) {
    val active=ls.count{isOnLeave(it)}
    val returned=ls.count{!isOnLeave(it)}
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("لوحة الإجازات",LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")))
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Orange)) {
            Row(Modifier.fillMaxWidth().padding(22.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Column {
                    Text("في إجازة حالياً",color=Color.White,fontSize=16.sp)
                    Text(active.toString(),fontSize=58.sp,fontWeight=FontWeight.Black,color=Color.White,lineHeight=62.sp)
                    Text("من إجمالي ${es.size} موظفاً",color=Color.White,fontSize=14.sp)
                }
                Column(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x44FFFFFF)).padding(15.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("سجلات",color=Color.White,fontSize=13.sp)
                    Text(ls.size.toString(),color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            DashboardMetric("عادوا خلال 30 يوماً",returned.toString(),MintInk,Modifier.weight(1f))
            DashboardMetric("إجمالي الموظفين",es.size.toString(),Orange,Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            DashboardMetric("سجلات الإجازات",ls.size.toString(),Orange,Modifier.weight(1f))
            DashboardMetric("بدون تاريخ عودة",ls.count{it.returnDate.isBlank()}.toString(),Muted,Modifier.weight(1f))
        }
        Text("يعودون قريباً",Modifier.padding(top=18.dp,bottom=9.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=16.dp)) {
            items(ls.filter{isOnLeave(it)}.sortedBy{it.returnDate}.take(5)){LeaveCard(it)}
        }
    }
}

@Composable private fun DashboardMetric(label:String,value:String,valueColor:Color,modifier:Modifier) {
    Card(modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(label,color=Muted,fontSize=13.sp,minLines=2)
            Text(value,color=valueColor,fontSize=30.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable private fun EmployeeCard(e:Employee,leaves:List<LeaveRecord>,onClick:()->Unit) {
    val initials=e.name.trim().firstOrNull()?.toString() ?: "؟"
    val onLeave=leaves.any{it.employeeCode==e.code && isOnLeave(it)}
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=CircleShape,color=Pale,modifier=Modifier.size(52.dp)) {
                Box(contentAlignment=Alignment.Center){Text(initials,fontSize=23.sp,fontWeight=FontWeight.Bold,color=Orange)}
            }
            Column(Modifier.weight(1f).padding(horizontal=12.dp),horizontalAlignment=Alignment.End) {
                Text(e.name,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1)
                Text("${e.job} · ${e.code}",fontSize=13.sp,color=Muted,maxLines=1)
            }
            Surface(shape=RoundedCornerShape(30.dp),color=if(onLeave) Pale else Mint) {
                Text(if(onLeave)"في إجازة" else "نشط",Modifier.padding(horizontal=12.dp,vertical=7.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(onLeave)Orange else MintInk)
            }
        }
    }
}

@Composable private fun LeaveCard(l:LeaveRecord) {
    val active=isOnLeave(l)
    val days=runCatching{ChronoUnit.DAYS.between(LocalDate.parse(l.returnDate),LocalDate.now()).toInt().coerceAtLeast(0)}.getOrDefault(0)
    val badgeColor=if(active) Pale else if(days<=30) BluePale else SoftGrey
    val badgeTextColor=if(active) Orange else if(days<=30) BlueInk else Muted
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
        Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Surface(shape=RoundedCornerShape(30.dp),color=badgeColor) {
                Text(if(active)"في إجازة" else if(l.returnDate.isBlank())"غير مسجل" else "عاد منذ $days يوماً",Modifier.padding(horizontal=12.dp,vertical=8.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=badgeTextColor)
            }
            Column(Modifier.weight(1f).padding(start=10.dp),horizontalAlignment=Alignment.End) {
                Text(l.employeeName,fontSize=17.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text("${l.job} · ${displayDate(l.startDate)} ← ${displayDate(l.returnDate)}",fontSize=13.sp,color=Muted)
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape=CircleShape,color=Pale,modifier=Modifier.size(48.dp)) {
                Box(contentAlignment=Alignment.Center){Text(l.employeeName.firstOrNull()?.toString() ?: "؟",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Orange)}
            }
        }
    }
}

@Composable private fun EmployeeProfile(e:Employee,history:List<LeaveRecord>,back:()->Unit,addLeave:()->Unit) {
    val days=history.mapNotNull{runCatching{ChronoUnit.DAYS.between(LocalDate.parse(it.returnDate),LocalDate.now()).toInt()}.getOrNull()}.maxOrNull() ?: 0
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("‹",Modifier.clickable{back()}.padding(8.dp),fontSize=30.sp,color=Muted)
            Text("ملف الموظف",fontSize=25.sp,fontWeight=FontWeight.ExtraBold,color=Ink)
            Spacer(Modifier.width(25.dp))
        }
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
            Column(Modifier.fillMaxWidth().padding(vertical=22.dp,horizontal=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Surface(shape=CircleShape,color=Pale,modifier=Modifier.size(94.dp)){Box(contentAlignment=Alignment.Center){Text(e.name.firstOrNull()?.toString() ?: "؟",fontSize=42.sp,fontWeight=FontWeight.Bold,color=Orange)}}
                Text(e.name,Modifier.padding(top=12.dp),fontSize=25.sp,fontWeight=FontWeight.ExtraBold,color=Ink,textAlign=TextAlign.Center)
                Text("${e.job} · الكود ${e.code}",Modifier.padding(top=6.dp),fontSize=15.sp,color=Muted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            ProfileMetric("إجازات",history.size.toString(),Modifier.weight(1f))
            ProfileMetric("يوماً منذ العودة",days.toString(),Modifier.weight(1f))
        }
        Text("تاريخ الإجازات",Modifier.fillMaxWidth().padding(top=18.dp,bottom=8.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink,textAlign=TextAlign.End)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)) { items(history){LeaveCard(it)} }
        Button(onClick=addLeave,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp).height(58.dp),shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.buttonColors(containerColor=Orange)) {
            Text("إضافة إجازة لهذا الموظف",fontSize=18.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable private fun ProfileMetric(label:String,value:String,modifier:Modifier) {
    Card(modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(value,fontSize=30.sp,fontWeight=FontWeight.Bold,color=Orange)
            Text(label,fontSize=13.sp,color=Muted)
        }
    }
}

private fun isOnLeave(l:LeaveRecord):Boolean = runCatching {
    val today=LocalDate.now()
    !today.isBefore(LocalDate.parse(l.startDate)) && !today.isAfter(LocalDate.parse(l.returnDate))
}.getOrDefault(false)

private fun displayDate(value:String):String = runCatching {
    LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(value)

@Composable fun EmployeeForm(save:(Employee)->Unit,close:()->Unit) {
    var c by remember{mutableStateOf("")};var n by remember{mutableStateOf("")};var j by remember{mutableStateOf("Life Guard")}
    AlertDialog(onDismissRequest=close,title={Text("إضافة موظف")},text={Column{OutlinedTextField(c,{c=it},label={Text("الكود")});OutlinedTextField(n,{n=it},label={Text("الاسم")});OutlinedTextField(j,{j=it},label={Text("الوظيفة")})}},
        confirmButton={TextButton(onClick={c.toIntOrNull()?.let{save(Employee(it,n,j));close()}},enabled=c.toIntOrNull()!=null&&n.isNotBlank()){Text("حفظ")}},
        dismissButton={TextButton(onClick=close){Text("إلغاء")}})
}

@Composable fun LeaveForm(es:List<Employee>,save:(Int,String,String,String)->Unit,close:()->Unit) {
    var c by remember{mutableStateOf("")};var s by remember{mutableStateOf(LocalDate.now().toString())};var r by remember{mutableStateOf(LocalDate.now().plusDays(10).toString())};var n by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=close,title={Text("إضافة إجازة")},text={Column{OutlinedTextField(c,{c=it},label={Text("كود الموظف")});es.find{it.code.toString()==c}?.let{Text(it.name,color=Orange)};OutlinedTextField(s,{s=it},label={Text("البداية YYYY-MM-DD")});OutlinedTextField(r,{r=it},label={Text("العودة YYYY-MM-DD")});OutlinedTextField(n,{n=it},label={Text("ملاحظات")})}},
        confirmButton={TextButton(onClick={c.toIntOrNull()?.let{save(it,s,r,n);close()}},enabled=es.any{it.code.toString()==c}){Text("حفظ")}},
        dismissButton={TextButton(onClick=close){Text("إلغاء")}})
}

@Composable fun ShiftPlanner(es:List<Employee>,ls:List<LeaveRecord>,all:List<ShiftAssignment>,save:(ShiftAssignment)->Unit,remove:(ShiftAssignment)->Unit) {
    var shift by remember{mutableIntStateOf(0)};var area by remember{mutableStateOf("Tirana Main Pool")};var code by remember{mutableStateOf("")}
    val today=LocalDate.now();val first=today.withDayOfMonth(1)
    val starts=listOf(first,first.withDayOfMonth(11),first.withDayOfMonth(21))
    val ends=listOf(first.withDayOfMonth(10),first.withDayOfMonth(20),first.withDayOfMonth(first.lengthOfMonth()))
    val start=starts[shift];val end=ends[shift];val current=all.filter{it.shiftStart==start.toString()}
    val emp=es.find{it.code.toString()==code}
    val onLeave=emp!=null&&ls.any{it.employeeCode==emp.code&&runCatching{!LocalDate.parse(it.startDate).isAfter(end)&&!LocalDate.parse(it.returnDate).isBefore(start)}.getOrDefault(false)}
    val duplicate=emp!=null&&current.any{it.employeeCode==emp.code}
    val limits=mapOf("Kafkas Pool" to 2,"Pool Bar" to 2,"Tirana Main Pool" to 9,"Tirana Family Pool" to 5,"Blue Planet Pool" to 10,"Relax Pool" to 1,"Technician" to 4)
    val capacity=limits[area];val full=capacity!=null&&current.count{it.area.equals(area,true)}>=capacity
    Column(Modifier.fillMaxSize()) {
        Text("الخطة السنوية · ${today.year}",color=Muted,fontSize=14.sp)
        Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("1–10","11–20","21–آخر الشهر").forEachIndexed{i,label->FilterPill("شيفت ${i+1} · $label",shift==i){shift=i}}
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=16.dp)) {
            items(limits.keys.toList()) { name ->
                val count=current.count{it.area.equals(name,true)}
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text(count.toString(),fontSize=28.sp,fontWeight=FontWeight.Bold,color=Orange)
                        Column(horizontalAlignment=Alignment.End) { Text(name,fontSize=17.sp,fontWeight=FontWeight.Bold,color=Ink);Text(current.filter{it.area.equals(name,true)}.joinToString("، "){it.employeeName}.ifBlank{"لا يوجد موظفون بعد"},fontSize=13.sp,color=Muted,maxLines=2) }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("إضافة موظف إلى منطقة",fontWeight=FontWeight.Bold,color=Ink,fontSize=18.sp)
                        OutlinedTextField(area,{area=it},Modifier.fillMaxWidth(),label={Text("اسم المنطقة")},singleLine=true)
                        OutlinedTextField(code,{code=it},Modifier.fillMaxWidth(),label={Text("كود الموظف")},singleLine=true)
                        emp?.let{Text("${it.name} · ${it.job}",color=Orange)}
                        if(onLeave) Text("الموظف لديه إجازة متداخلة",color=Orange)
                        if(duplicate) Text("الموظف مضاف بالفعل",color=Orange)
                        if(full) Text("اكتمل العدد المطلوب ($capacity)",color=Orange)
                        Button(onClick={emp?.let{save(ShiftAssignment(shiftStart=start.toString(),shiftEnd=end.toString(),area=area,employeeCode=it.code,employeeName=it.name));code=""}},enabled=emp!=null&&area.isNotBlank()&&!onLeave&&!duplicate&&!full,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Orange)){Text("إسناد الموظف")}
                    }
                }
            }
            item { Text("الموظفون المسندون",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink) }
            items(current) { a ->
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Border)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        TextButton(onClick={remove(a)}){Text("حذف",color=Orange)}
                        Column(horizontalAlignment=Alignment.End){Text(a.employeeName,fontWeight=FontWeight.Bold,color=Ink);Text("${a.area} · ${a.employeeCode}",fontSize=12.sp,color=Muted)}
                    }
                }
            }
        }
    }
}
