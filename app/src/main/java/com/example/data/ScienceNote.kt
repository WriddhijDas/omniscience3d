package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "science_notes")
data class ScienceNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String, // "Physics", "Chemistry", "Biology", "Computer Science", "Mathematics"
    val modelKey: String, // e.g., "PlanetaryOrbit", "Helix", "Molecules", "BinaryTree", "Surface3D"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

@Dao
interface ScienceNoteDao {
    @Query("SELECT * FROM science_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<ScienceNote>>

    @Query("SELECT * FROM science_notes WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedNotes(): Flow<List<ScienceNote>>

    @Query("SELECT * FROM science_notes WHERE subject = :subject ORDER BY timestamp DESC")
    fun getNotesBySubject(subject: String): Flow<List<ScienceNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ScienceNote)

    @Delete
    suspend fun deleteNote(note: ScienceNote)

    @Query("UPDATE science_notes SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: Long, isBookmarked: Boolean)
}

@Database(entities = [ScienceNote::class], version = 1, exportSchema = false)
abstract class ScienceDatabase : RoomDatabase() {
    abstract fun scienceNoteDao(): ScienceNoteDao
}

class ScienceRepository(private val dao: ScienceNoteDao) {
    val allNotes: Flow<List<ScienceNote>> = dao.getAllNotes()
    val bookmarkedNotes: Flow<List<ScienceNote>> = dao.getBookmarkedNotes()

    fun getNotesBySubject(subject: String): Flow<List<ScienceNote>> = dao.getNotesBySubject(subject)

    suspend fun saveNote(note: ScienceNote) = dao.insertNote(note)

    suspend fun deleteNote(note: ScienceNote) = dao.deleteNote(note)

    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean) = dao.updateBookmarkStatus(id, isBookmarked)
}
