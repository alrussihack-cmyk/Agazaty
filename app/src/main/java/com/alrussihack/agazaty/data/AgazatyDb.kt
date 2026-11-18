package com.alrussihack.agazaty.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="employees") data class Employee(@PrimaryKey val code:Int,val name:String,val job:String)
@Entity(tableName="leaves",indices=[Index("employeeCode")]) data class LeaveRecord(@PrimaryKey(autoGenerate=true) val id:Long=0,val employeeCode:Int,val employeeName:String,val job:String,val startDate:String,val returnDate:String,val notes:String="")
@Entity(tableName="shift_assignments", indices=[Index("shiftStart"),Index("employeeCode")])
data class ShiftAssignment(@PrimaryKey(autoGenerate=true) val id:Long=0,val shiftStart:String,val shiftEnd:String,val area:String,val employeeCode:Int,val employeeName:String)
@Dao interface AgazatyDao {
 @Query("SELECT * FROM employees ORDER BY name COLLATE NOCASE") fun employees():Flow<List<Employee>>
 @Query("SELECT * FROM leaves ORDER BY startDate DESC,id DESC") fun leaves():Flow<List<LeaveRecord>>
 @Query("SELECT * FROM employees WHERE code=:code LIMIT 1") suspend fun employee(code:Int):Employee?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertEmployee(e:Employee)
 @Insert suspend fun insertLeave(l:LeaveRecord)
 @Query("SELECT COUNT(*) FROM employees") suspend fun employeeCount():Int
 @Query("SELECT COUNT(*) FROM leaves") suspend fun leaveCount():Int
 @Query("SELECT * FROM shift_assignments ORDER BY shiftStart DESC,area,employeeName") fun assignments():Flow<List<ShiftAssignment>>
 @Insert suspend fun insertAssignment(a:ShiftAssignment)
 @Delete suspend fun deleteAssignment(a:ShiftAssignment)
}
@Database(entities=[Employee::class,LeaveRecord::class,ShiftAssignment::class],version=2,exportSchema=false)
abstract class AgazatyDb:RoomDatabase(){
 abstract fun dao():AgazatyDao
 companion object { @Volatile private var instance:AgazatyDb?=null
  private val MIGRATION_1_2=object:androidx.room.migration.Migration(1,2){ override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase){db.execSQL("CREATE TABLE IF NOT EXISTS shift_assignments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, shiftStart TEXT NOT NULL, shiftEnd TEXT NOT NULL, area TEXT NOT NULL, employeeCode INTEGER NOT NULL, employeeName TEXT NOT NULL)");db.execSQL("CREATE INDEX IF NOT EXISTS index_shift_assignments_shiftStart ON shift_assignments(shiftStart)");db.execSQL("CREATE INDEX IF NOT EXISTS index_shift_assignments_employeeCode ON shift_assignments(employeeCode)") } }
 fun get(context:android.content.Context):AgazatyDb=instance?: synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,AgazatyDb::class.java,"agazaty.db").addMigrations(MIGRATION_1_2).build().also{instance=it}}
 }
}