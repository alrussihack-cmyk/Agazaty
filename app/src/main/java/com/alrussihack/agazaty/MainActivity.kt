package com.alrussihack.agazaty
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.alrussihack.agazaty.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import org.json.JSONObject

private val cream=Color(0xFFFFF8F1); private val orange=Color(0xFFC83D08); private val ink=Color(0xFF302015); private val muted=Color(0xFF795F4C); private val line=Color(0xFFF3E0D0)
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val db=AgazatyDb.get(this)
 lifecycleScope.launch { seedDatabase(db) }
 setContent{val es by db.dao().employees().collectAsStateWithLifecycle(emptyList());val ls by db.dao().leaves().collectAsStateWithLifecycle(emptyList());AgazatyUI(es,ls,{e->lifecycleScope.launch{db.dao().insertEmployee(e)}},{c,s,r,n->lifecycleScope.launch{db.dao().employee(c)?.let{db.dao().insertLeave(LeaveRecord(employeeCode=c,employeeName=it.name,job=it.job,startDate=s,returnDate=r,notes=n))}}})}
 }
 private suspend fun seedDatabase(db:AgazatyDb) {
  val data=JSONObject(assets.open("seed.json").bufferedReader().use{it.readText()})
  val employees=data.getJSONArray("employees")
  for(i in 0 until employees.length()) {
   val e=employees.getJSONObject(i)
   db.dao().insertEmployee(Employee(e.getInt("code"),e.getString("name"),e.getString("job")))
  }
  if(db.dao().leaveCount()==0) {
   val leaves=data.getJSONArray("leaves")
   for(i in 0 until leaves.length()) {
    val l=leaves.getJSONObject(i)
    db.dao().insertLeave(LeaveRecord(employeeCode=l.getInt("code"),employeeName=l.getString("name"),job=l.getString("job"),startDate=l.getString("start"),returnDate=l.getString("returnDate")))
   }
  }
 }
}
@Composable fun AgazatyUI(es:List<Employee>,ls:List<LeaveRecord>,addE:(Employee)->Unit,addL:(Int,String,String,String)->Unit){
 var tab by remember{mutableIntStateOf(0)};var query by remember{mutableStateOf("")};var addEmp by remember{mutableStateOf(false)};var addLeave by remember{mutableStateOf(false)}
 CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl){
 MaterialTheme(colorScheme=lightColorScheme(primary=orange,background=cream,surface=Color.White,onSurface=ink)){
 Scaffold(containerColor=cream,bottomBar={NavigationBar(containerColor=Color.White){listOf("الرئيسية","الإجازات","الشيفتات","الموظفون").forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={},label={Text(t)},colors=NavigationBarItemDefaults.colors(selectedTextColor=orange,selectedIconColor=orange))}}},floatingActionButton={if(tab==1||tab==3)FloatingActionButton(onClick={if(tab==1)addLeave=true else addEmp=true},containerColor=orange,contentColor=Color.White){Text("+",fontSize=28.sp)}}){p->
 Column(Modifier.fillMaxSize().padding(p).padding(horizontal=18.dp)){
 Text(listOf("لوحة الإجازات","سجل الإجازات","توزيع الشيفتات","الموظفون")[tab],Modifier.padding(top=22.dp,bottom=4.dp),fontSize=27.sp,fontWeight=FontWeight.ExtraBold,color=ink)
 Text("إجازاتي · يعمل محلياً دون إنترنت",color=muted,modifier=Modifier.padding(bottom=16.dp))
 if(tab==0){val active=ls.count{runCatching{!LocalDate.now().isBefore(LocalDate.parse(it.startDate))&&!LocalDate.now().isAfter(LocalDate.parse(it.returnDate))}.getOrDefault(false)}
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=orange)){Column(Modifier.padding(22.dp)){Text("في إجازة حالياً",color=Color.White);Text(active.toString(),fontSize=54.sp,color=Color.White,fontWeight=FontWeight.Black);Text("من إجمالي "+es.size+" موظف",color=Color.White)}}
 Row(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("الموظفون",es.size,Modifier.weight(1f));Metric("سجلات الإجازات",ls.size,Modifier.weight(1f))}
 Text("أقرب العائدين",Modifier.padding(top=18.dp,bottom=8.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=ink)
 LazyColumn{items(ls.take(8)){LeaveItem(it)}}
 }else if(tab==2){listOf("الرئيسي" to 5,"بلوبلانت" to 7,"العائلي" to 4,"كافكاس" to 2,"بول بار" to 2,"ريلاكس" to 1).forEach{Metric(it.first,it.second,Modifier.fillMaxWidth().padding(bottom=8.dp))};Text("ملخص المناطق التشغيلية. إسناد الأفراد للشيفتات قيد التطوير.",color=muted)}
 else {OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(bottom=12.dp),placeholder={Text("ابحث بالاسم أو الكود")},singleLine=true)
 if(tab==3){LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(es.filter{it.name.contains(query,true)||it.code.toString().contains(query)||it.job.contains(query,true)}){EmployeeItem(it)}}}
 else{LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(ls.filter{it.employeeName.contains(query,true)||it.employeeCode.toString().contains(query)}){LeaveItem(it)}}}}
 }
 }}
 }
 if(addEmp) EmployeeForm({addE(it);addEmp=false},{addEmp=false})
 if(addLeave) LeaveForm(es,{c,s,r,n->addL(c,s,r,n);addLeave=false},{addLeave=false})
}
@Composable fun Metric(label:String,value:Int,modifier:Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,line)){Column(Modifier.padding(16.dp)){Text(label,color=muted,fontSize=13.sp);Text(value.toString(),fontSize=30.sp,color=orange,fontWeight=FontWeight.Bold)}}}
@Composable fun EmployeeItem(e:Employee){Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,line)){Column(Modifier.padding(15.dp)){Text(e.name,fontWeight=FontWeight.Bold,color=ink);Text(e.job+" · "+e.code,color=muted,fontSize=13.sp)}}}
@Composable fun LeaveItem(l:LeaveRecord){Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,line)){Column(Modifier.padding(15.dp)){Text(l.employeeName,fontWeight=FontWeight.Bold,color=ink);Text(l.job+" · "+l.startDate+" ← "+l.returnDate,color=muted,fontSize=13.sp)}}}
@Composable fun EmployeeForm(save:(Employee)->Unit,close:()->Unit){var c by remember{mutableStateOf("")};var n by remember{mutableStateOf("")};var j by remember{mutableStateOf("Life Guard")};AlertDialog(onDismissRequest=close,title={Text("إضافة موظف")},text={Column{OutlinedTextField(c,{c=it},label={Text("الكود")});OutlinedTextField(n,{n=it},label={Text("الاسم")});OutlinedTextField(j,{j=it},label={Text("الوظيفة")})}},confirmButton={TextButton(onClick={c.toIntOrNull()?.let{save(Employee(it,n,j));close()}},enabled=c.toIntOrNull()!=null&&n.isNotBlank()){Text("حفظ")}},dismissButton={TextButton(onClick=close){Text("إلغاء")}})}
@Composable fun LeaveForm(es:List<Employee>,save:(Int,String,String,String)->Unit,close:()->Unit){var c by remember{mutableStateOf("")};var s by remember{mutableStateOf(LocalDate.now().toString())};var r by remember{mutableStateOf(LocalDate.now().plusDays(10).toString())};var n by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("إضافة إجازة")},text={Column{OutlinedTextField(c,{c=it},label={Text("كود الموظف")});es.find{it.code.toString()==c}?.let{Text(it.name,color=orange)};OutlinedTextField(s,{s=it},label={Text("البداية YYYY-MM-DD")});OutlinedTextField(r,{r=it},label={Text("العودة YYYY-MM-DD")});OutlinedTextField(n,{n=it},label={Text("ملاحظات")})}},confirmButton={TextButton(onClick={c.toIntOrNull()?.let{save(it,s,r,n);close()}},enabled=es.any{it.code.toString()==c}){Text("حفظ")}},dismissButton={TextButton(onClick=close){Text("إلغاء")}})}
