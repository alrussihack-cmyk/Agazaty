package com.alrussihack.agazaty.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "employees")
data class Employee(@PrimaryKey val code: Int, val name: String, val job: String)

/**
 * startDate / returnDate (expected return) / actualReturnDate are ISO yyyy-MM-dd strings, "" when unknown.
 * actualReturnDate is what flips a leave from "في إجازة" to "عاد".
 */
@Entity(tableName = "leaves", indices = [Index("employeeCode")])
data class LeaveRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeCode: Int,
    val employeeName: String,
    val job: String,
    val startDate: String,
    val returnDate: String,
    val notes: String = "",
    val type: String = "سنوية",
    val actualReturnDate: String = ""
)

@Entity(tableName = "shift_assignments", indices = [Index("shiftStart"), Index("employeeCode")])
data class ShiftAssignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftStart: String,
    val shiftEnd: String,
    val area: String,
    val employeeCode: Int,
    val employeeName: String
)

@Dao
interface AgazatyDao {
    @Query("SELECT * FROM employees ORDER BY name COLLATE NOCASE") fun employees(): Flow<List<Employee>>
    @Query("SELECT * FROM leaves ORDER BY startDate DESC, id DESC") fun leaves(): Flow<List<LeaveRecord>>
    @Query("SELECT * FROM shift_assignments ORDER BY shiftStart DESC, area, employeeName") fun assignments(): Flow<List<ShiftAssignment>>

    @Query("SELECT * FROM employees WHERE code=:code LIMIT 1") suspend fun employee(code: Int): Employee?
    @Query("SELECT COUNT(*) FROM employees") suspend fun employeeCount(): Int
    @Query("SELECT COUNT(*) FROM leaves") suspend fun leaveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEmployee(e: Employee)
    @Insert suspend fun insertLeave(l: LeaveRecord)
    @Update suspend fun updateLeave(l: LeaveRecord)
    @Delete suspend fun deleteLeave(l: LeaveRecord)
    @Insert suspend fun insertAssignment(a: ShiftAssignment)
    @Delete suspend fun deleteAssignment(a: ShiftAssignment)
}

@Database(
    entities = [Employee::class, LeaveRecord::class, ShiftAssignment::class],
    version = 3,
    exportSchema = false
)
abstract class AgazatyDb : RoomDatabase() {
    abstract fun dao(): AgazatyDao

    companion object {
        @Volatile private var instance: AgazatyDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS shift_assignments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, shiftStart TEXT NOT NULL, shiftEnd TEXT NOT NULL, area TEXT NOT NULL, employeeCode INTEGER NOT NULL, employeeName TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shift_assignments_shiftStart ON shift_assignments(shiftStart)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shift_assignments_employeeCode ON shift_assignments(employeeCode)")
            }
        }

        /** Adds leave type + actual return date. Past return dates already imported are treated as actual returns. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leaves ADD COLUMN type TEXT NOT NULL DEFAULT 'سنوية'")
                db.execSQL("ALTER TABLE leaves ADD COLUMN actualReturnDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE leaves SET actualReturnDate = returnDate WHERE returnDate <> '' AND returnDate <= date('now','localtime')")
            }
        }

        fun get(context: android.content.Context): AgazatyDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AgazatyDb::class.java, "agazaty.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
