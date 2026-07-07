package com.focusremind.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerAt: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: Int = 0, // 0=normal, 1=high, 2=urgent
    val nagMode: Boolean = false,
    val isVoiceCreated: Boolean = false,
    val originalVoiceText: String? = null,
    val photoUri: String? = null,
    val recurrence: String? = null, // null = one-time, "DAILY", "WEEKLY"
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerAt ASC")
    fun getActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY completedAt DESC LIMIT 50")
    fun getCompleted(): Flow<List<Reminder>>

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 1 AND completedAt > :since")
    fun getCompletedCountSince(since: Long): Flow<Int>

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Query("UPDATE reminders SET isCompleted = 1, completedAt = :now WHERE id = :id")
    suspend fun complete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET triggerAt = :newTime WHERE id = :id")
    suspend fun snooze(id: Long, newTime: Long)

    @Query("UPDATE reminders SET title = :title, triggerAt = :triggerAt WHERE id = :id")
    suspend fun update(id: Long, title: String, triggerAt: Long)

    @Query("UPDATE reminders SET photoUri = :uri WHERE id = :id")
    suspend fun updatePhoto(id: Long, uri: String?)

    @Query("UPDATE reminders SET recurrence = :recurrence WHERE id = :id")
    suspend fun updateRecurrence(id: Long, recurrence: String?)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?
}

@Database(entities = [Reminder::class], version = 4)
abstract class FocusRemindDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
