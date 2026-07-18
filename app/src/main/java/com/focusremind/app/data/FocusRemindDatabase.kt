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
    val nagMode: Boolean = false,
    val isVoiceCreated: Boolean = false,
    val originalVoiceText: String? = null,
    val photoUri: String? = null,
    val recurrence: String? = null, // null = one-time, "DAILY", "WEEKLY"
    // The "canonical" scheduled time for a recurring reminder's cycle —
    // separate from triggerAt so that snoozing (+15/+30 min) only delays
    // THIS instance without permanently shifting the whole recurring
    // schedule. Only advanced when a cycle genuinely completes (the alarm
    // fires normally), never touched by a snooze. Null for one-time reminders.
    val anchorTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inCart: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerAt ASC")
    fun getActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE recurrence IS NOT NULL ORDER BY triggerAt ASC")
    fun getRecurring(): Flow<List<Reminder>>

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

    // Used ONLY when a recurring reminder's cycle genuinely advances (the
    // alarm fired normally) — updates both triggerAt AND the anchor, unlike
    // snooze() which deliberately leaves the anchor untouched.
    @Query("UPDATE reminders SET triggerAt = :newTime, anchorTime = :newTime WHERE id = :id")
    suspend fun advanceRecurrence(id: Long, newTime: Long)

    @Query("UPDATE reminders SET title = :title, triggerAt = :triggerAt WHERE id = :id")
    suspend fun update(id: Long, title: String, triggerAt: Long)

    // Used when the user manually edits an EXISTING recurring reminder's
    // title/time — a deliberate re-anchor, unlike snooze() which must never
    // touch the anchor.
    @Query("UPDATE reminders SET title = :title, triggerAt = :triggerAt, anchorTime = :triggerAt WHERE id = :id")
    suspend fun updateRecurringDetails(id: Long, title: String, triggerAt: Long)

    @Query("UPDATE reminders SET photoUri = :uri WHERE id = :id")
    suspend fun updatePhoto(id: Long, uri: String?)

    @Query("UPDATE reminders SET recurrence = :recurrence WHERE id = :id")
    suspend fun updateRecurrence(id: Long, recurrence: String?)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items WHERE inCart = 0 ORDER BY addedAt ASC")
    fun getToBuy(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE inCart = 1 ORDER BY addedAt ASC")
    fun getInCart(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): ShoppingItem?

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Query("UPDATE shopping_items SET inCart = :inCart WHERE id = :id")
    suspend fun setInCart(id: Long, inCart: Boolean)

    @Query("UPDATE shopping_items SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items WHERE inCart = 0")
    suspend fun clearToBuy()

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()
}

@Database(entities = [Reminder::class, ShoppingItem::class], version = 7)
abstract class FocusRemindDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun shoppingDao(): ShoppingDao
}
