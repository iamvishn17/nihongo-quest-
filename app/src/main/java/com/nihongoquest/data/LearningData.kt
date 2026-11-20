package com.nihongoquest.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lessons")
data class Lesson(@PrimaryKey(autoGenerate = true) val id: Int = 0, val title: String, val description: String = "", val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "vocabulary")
data class Vocabulary(
 @PrimaryKey(autoGenerate = true) val id: Long = 0, val japanese: String, val reading: String = "", val romaji: String = "", val meaning: String,
 val example: String = "", val exampleMeaning: String = "", val jlpt: String = "N5", val lesson: Int = 0, val category: String = "General",
 val notes: String = "", val attempts: Int = 0, val correct: Int = 0, val nextReview: Long = 0, val lastReviewed: Long = 0
)
@Entity(tableName = "kanji")
data class Kanji(@PrimaryKey(autoGenerate = true) val id: Long = 0, val character: String, val meaning: String, val onyomi: String = "", val kunyomi: String = "", val jlpt: String = "N5", val notes: String = "")
@Entity(tableName = "study_events")
data class StudyEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0, val timestamp: Long = System.currentTimeMillis(), val correct: Boolean, val itemType: String = "vocabulary", val durationSeconds: Int = 0)
@Entity(tableName = "daily_tasks")
data class DailyTask(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "daily_task_checks", primaryKeys = ["taskId", "day"])
data class DailyTaskCheck(val taskId: Long, val day: String, val done: Boolean)
@Entity(tableName = "daily_progress", primaryKeys = ["itemId", "mode", "day"])
data class DailyProgress(val itemId: Long, val mode: String, val day: String, val status: String)

@Dao
interface LearningDao {
 @Query("SELECT * FROM lessons ORDER BY createdAt DESC") fun observeLessons(): Flow<List<Lesson>>
 @Insert suspend fun insertLesson(item: Lesson): Long
 @Delete suspend fun deleteLesson(item: Lesson)
 @Query("SELECT * FROM vocabulary ORDER BY japanese COLLATE NOCASE") fun observeVocabulary(): Flow<List<Vocabulary>>
 @Query("SELECT * FROM vocabulary WHERE id = :id LIMIT 1") suspend fun getVocabulary(id: Long): Vocabulary?
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVocabulary(item: Vocabulary): Long
 @Update suspend fun updateVocabulary(item: Vocabulary)
 @Delete suspend fun deleteVocabulary(item: Vocabulary)
 @Query("SELECT * FROM kanji ORDER BY character") fun observeKanji(): Flow<List<Kanji>>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertKanji(item: Kanji): Long
 @Delete suspend fun deleteKanji(item: Kanji)
 @Insert suspend fun addEvent(event: StudyEvent)
 @Query("SELECT * FROM study_events WHERE timestamp >= :from ORDER BY timestamp DESC") fun observeEventsSince(from: Long): Flow<List<StudyEvent>>
 @Query("SELECT COUNT(*) FROM vocabulary") fun vocabularyCount(): Flow<Int>
 @Query("SELECT COUNT(*) FROM kanji") fun kanjiCount(): Flow<Int>
 @Query("SELECT COUNT(*) FROM vocabulary WHERE nextReview <= :now") fun dueCount(now: Long): Flow<Int>
 @Query("SELECT * FROM daily_tasks ORDER BY createdAt DESC") fun observeTasks(): Flow<List<DailyTask>>
 @Insert suspend fun insertTask(task: DailyTask): Long
 @Delete suspend fun deleteTask(task: DailyTask)
 @Query("SELECT * FROM daily_task_checks WHERE day = :day") fun observeTaskChecks(day: String): Flow<List<DailyTaskCheck>>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun setTaskCheck(check: DailyTaskCheck)
 @Query("SELECT * FROM daily_progress WHERE day = :day AND mode = :mode") fun observeDailyProgress(day: String, mode: String): Flow<List<DailyProgress>>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun setDailyProgress(progress: DailyProgress)
 @Query("DELETE FROM daily_progress WHERE day != :day") suspend fun clearOldDailyProgress(day: String)
}

@Database(entities = [Lesson::class, Vocabulary::class, Kanji::class, StudyEvent::class, DailyTask::class, DailyTaskCheck::class, DailyProgress::class], version = 2, exportSchema = false)
abstract class LearningDatabase : RoomDatabase() {
 abstract fun learningDao(): LearningDao
 companion object {
  @Volatile private var INSTANCE: LearningDatabase? = null
  private val MIGRATION_1_2 = object : Migration(1, 2) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE IF NOT EXISTS lessons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    db.execSQL("INSERT INTO lessons (id, title, description, createdAt) VALUES (1, 'My first lesson', 'Your existing vocabulary', 0)")
    db.execSQL("UPDATE vocabulary SET lesson = 1 WHERE lesson = 0")
    db.execSQL("CREATE TABLE IF NOT EXISTS daily_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    db.execSQL("CREATE TABLE IF NOT EXISTS daily_task_checks (taskId INTEGER NOT NULL, day TEXT NOT NULL, done INTEGER NOT NULL, PRIMARY KEY(taskId, day))")
    db.execSQL("CREATE TABLE IF NOT EXISTS daily_progress (itemId INTEGER NOT NULL, mode TEXT NOT NULL, day TEXT NOT NULL, status TEXT NOT NULL, PRIMARY KEY(itemId, mode, day))")
   }
  }
  fun get(context: Context): LearningDatabase = INSTANCE ?: synchronized(this) {
   INSTANCE ?: Room.databaseBuilder(context.applicationContext, LearningDatabase::class.java, "nihongo_quest.db").addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
  }
 }
}
