package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AwaySession
import kotlinx.coroutines.flow.Flow

@Dao
interface AwaySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AwaySession): Long

    @Update
    suspend fun updateSession(session: AwaySession)

    @Delete
    suspend fun deleteSession(session: AwaySession)

    @Query("DELETE FROM away_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM away_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM away_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): AwaySession?

    @Query("SELECT * FROM away_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): Flow<AwaySession?>

    @Query("SELECT * FROM away_sessions WHERE dateKey = :dateKey AND isActive = 0 ORDER BY startTime DESC")
    fun getSessionsForDate(dateKey: String): Flow<List<AwaySession>>

    @Query("SELECT * FROM away_sessions WHERE dateKey = :dateKey AND isActive = 0 ORDER BY startTime DESC")
    suspend fun getSessionsForDateSync(dateKey: String): List<AwaySession>

    @Query("SELECT * FROM away_sessions WHERE dateKey IN (:dateKeys) AND isActive = 0 ORDER BY startTime DESC")
    fun getSessionsForDateKeys(dateKeys: List<String>): Flow<List<AwaySession>>

    @Query("SELECT * FROM away_sessions WHERE dateKey IN (:dateKeys) AND isActive = 0 ORDER BY startTime DESC")
    suspend fun getSessionsForDateKeysSync(dateKeys: List<String>): List<AwaySession>

    @Query("SELECT * FROM away_sessions WHERE isActive = 0 ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<AwaySession>>

    @Query("SELECT * FROM away_sessions WHERE isActive = 0 ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<AwaySession>

    @Query("SELECT * FROM away_sessions WHERE isActive = 0 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<AwaySession>>

    @Query("SELECT * FROM away_sessions WHERE isActive = 0 ORDER BY durationMillis DESC LIMIT 1")
    suspend fun getLongestSessionAllTime(): AwaySession?

    @Query("DELETE FROM away_sessions WHERE durationMillis <= 0 AND isActive = 0")
    suspend fun cleanInvalidRecords()
}
