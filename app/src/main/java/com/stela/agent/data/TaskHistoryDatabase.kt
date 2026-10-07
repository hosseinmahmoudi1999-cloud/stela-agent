package com.stela.agent.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val prompt: String,
    val status: String,
    val summary: String,
    val durationMs: Long,
    val errorSummary: String?
)

@Dao
interface TaskHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaskHistoryEntity)

    @Query("SELECT * FROM task_history ORDER BY timestamp DESC LIMIT 50")
    suspend fun recent(): List<TaskHistoryEntity>
}

@Database(entities = [TaskHistoryEntity::class], version = 1, exportSchema = false)
abstract class TaskHistoryDatabase : RoomDatabase() {
    abstract fun taskHistoryDao(): TaskHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TaskHistoryDatabase? = null

        fun getDatabase(context: Context): TaskHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskHistoryDatabase::class.java,
                    "stela_task_history.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
