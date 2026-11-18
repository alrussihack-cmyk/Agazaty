package com.alrussihack.agazaty.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="employees") data class Employee(@PrimaryKey val code:Int,val name:String,val job:String)
@Entity(tableName="leaves",indices=[Index("employeeCode")]) data class LeaveRecord(@PrimaryKey(autoGenerate=true) val id:Long=0,val employeeCode:Int,val employeeName:String,val job:String,val startDate:String,val returnDate:String,val notes:String="")
@Dao interface AgazatyDao {
 @Query("SELECT * FROM employees ORDER BY name COLLATE NOCASE") fun employees():Flow<List<Employee>>
 @Query("SELECT * FROM leaves ORDER BY startDate DESC,id DESC") fun leaves():Flow<List<LeaveRecord>>
 @Query("SELECT * FROM employees WHERE code=:code LIMIT 1") suspend fun employee(code:Int):Employee?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertEmployee(e:Employee)
 @Insert suspend fun insertLeave(l:LeaveRecord)
 @Query("SELECT COUNT(*) FROM employees") suspend fun employeeCount():Int
}
@Database(entities=[Employee::class,LeaveRecord::class],version=1,exportSchema=false)
abstract class AgazatyDb:RoomDatabase(){
 abstract fun dao():AgazatyDao
 companion object { @Volatile private var instance:AgazatyDb?=null
 fun get(context:android.content.Context):AgazatyDb=instance?: synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,AgazatyDb::class.java,"agazaty.db").build().also{instance=it}}
 }
}