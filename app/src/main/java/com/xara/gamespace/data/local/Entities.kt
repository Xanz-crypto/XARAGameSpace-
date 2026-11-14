package com.xara.gamespace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isAddedToSpace: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val totalPlaytimeSeconds: Long = 0L,
    val boostModeOverride: String? = null,
    val graphicsDriver: String = "DEFAULT",
    val textureOptMode: String = "SHARP_NATIVE",
    val sensitivityX: Float = 1.0f,
    val sensitivityY: Float = 1.0f,
    val compileStatus: String = "Not Compiled",
    val lastCompiledTimestamp: Long = 0L
)

@Entity(tableName = "boost_history")
data class BoostHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mode: String,
    val beforeUsedRamMb: Long,
    val afterUsedRamMb: Long,
    val freedRamMb: Long,
    val processesInspected: Int,
    val processesCleared: Int,
    val recommendationsSummary: String,
    val timestamp: Long
)

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gamePackage: String?,
    val gameName: String,
    val boostMode: String,
    val durationSeconds: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val initialTempC: Float,
    val peakTempC: Float,
    val batteryDeltaPct: Int,
    val avgRamPercent: Int
)

@Entity(tableName = "touch_profiles")
data class TouchProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val gamePackage: String?,
    val sensitivityX: Float,
    val sensitivityY: Float,
    val touchAreaScale: Float,
    val touchZoneMode: String,
    val isPreset: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "macro_configs")
data class MacroConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val gamePackage: String?,
    val triggerX: Float,
    val triggerY: Float,
    val targetX: Float,
    val targetY: Float,
    val triggerSizeDp: Float,
    val repeatIntervalMs: Int,
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
