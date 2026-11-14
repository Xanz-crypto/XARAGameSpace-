package com.xara.gamespace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface XaraDao {
    @Query("SELECT * FROM game_profiles")
    fun observeGameProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg LIMIT 1")
    suspend fun getGameProfile(pkg: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameProfile(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles")
    suspend fun clearGameProfiles()

    @Query("SELECT * FROM boost_history ORDER BY timestamp DESC LIMIT 30")
    fun observeBoostHistory(): Flow<List<BoostHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoostHistory(item: BoostHistoryEntity)

    @Query("DELETE FROM boost_history")
    suspend fun clearBoostHistory()

    @Query("SELECT * FROM game_sessions ORDER BY startTimestamp DESC LIMIT 50")
    fun observeGameSessions(): Flow<List<GameSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameSession(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions")
    suspend fun clearGameSessions()

    @Query("SELECT * FROM touch_profiles ORDER BY isPreset DESC, updatedAt DESC")
    fun observeTouchProfiles(): Flow<List<TouchProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTouchProfile(profile: TouchProfileEntity)

    @Query("DELETE FROM touch_profiles WHERE id = :id")
    suspend fun deleteTouchProfile(id: Long)

    @Query("DELETE FROM touch_profiles WHERE isPreset = 0")
    suspend fun clearCustomTouchProfiles()

    @Query("SELECT * FROM macro_configs ORDER BY updatedAt DESC")
    fun observeMacroConfigs(): Flow<List<MacroConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMacroConfig(config: MacroConfigEntity)

    @Query("DELETE FROM macro_configs WHERE id = :id")
    suspend fun deleteMacroConfig(id: Long)

    @Query("DELETE FROM macro_configs")
    suspend fun clearMacroConfigs()
}
