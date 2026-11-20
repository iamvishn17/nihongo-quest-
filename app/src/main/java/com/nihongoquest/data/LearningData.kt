package com.nihongoquest.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vocabulary")
data class Vocabulary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val japanese: String,
    val reading: String = "",
    val romaji: String = "",
    val meaning: String,
    val example: String = "",
    val exampleMeaning: String = "",
    val jlpt: String = "N5",
    val lesson: Int = 0,
    val category: String = "General",
    val notes: String = "",
    val attempts: Int = 0,
    val correct: Int = 0,
    val nextReview: Long = 0,
    val lastReviewed: Long = 0
)
@Entity(tableName = "kanji")
data class Kanji(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val character: String,
    val meaning: String,
    val onyomi: String = "",
    val kunyomi: String = "",
    val jlpt: String = "N5",
    val notes: String = ""
)
@Entity(tableName = "study_events")
data class StudyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val correct: Boolean,
    val itemType: String = "vocabulary",
    val durationSeconds: Int = 0
)
@Dao
interface LearningDao {
    @Query("SELECT * FROM vocabulary ORDER BY japanese COLLATE NOCASE")
    fun observeVocabulary(): Flow<List<Vocabulary>>
    @Query("SELECT * FROM vocabulary WHERE id = :id LIMIT 1")
    suspend fun getVocabulary(id: Long): Vocabulary?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(item: Vocabulary): Long
    @Update
    suspend fun updateVocabulary(item: Vocabulary)
    @Delete
    suspend fun deleteVocabulary(item: Vocabulary)
    @Query("SELECT * FROM kanji ORDER BY character")
    fun observeKanji(): Flow<List<Kanji>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanji(item: Kanji): Long
    @Delete
    suspend fun deleteKanji(item: Kanji)
    @Insert
    suspend fun addEvent(event: StudyEvent)
    @Query("SELECT * FROM study_events WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun observeEventsSince(from: Long): Flow<List<StudyEvent>>
    @Query("SELECT COUNT(*) FROM vocabulary")
    fun vocabularyCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM kanji")
    fun kanjiCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM vocabulary WHERE nextReview <= :now")
    fun dueCount(now: Long): Flow<Int>
}
@Database(entities = [Vocabulary::class, Kanji::class, StudyEvent::class], version = 1, exportSchema = false)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
    companion object {
        @Volatile private var INSTANCE: LearningDatabase? = null
        fun get(context: Context): LearningDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, LearningDatabase::class.java, "nihongo_quest.db")
                .build().also { INSTANCE = it }
        }
    }
}
