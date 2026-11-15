package com.xara.gamespace.data.repository

import com.xara.gamespace.data.local.BoostHistoryEntity
import com.xara.gamespace.data.local.GameProfileEntity
import com.xara.gamespace.data.local.GameSessionEntity
import com.xara.gamespace.data.local.MacroConfigEntity
import com.xara.gamespace.data.local.TouchProfileEntity
import com.xara.gamespace.data.local.XaraDao
import com.xara.gamespace.data.preferences.XaraPreferences
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.FloatingPositionPreset
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.TextureOptMode
import com.xara.gamespace.domain.model.ThemeAccent
import com.xara.gamespace.domain.model.TouchZoneMode
import com.xara.gamespace.domain.model.XaraUserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class XaraRepository(
    private val dao: XaraDao,
    private val preferences: XaraPreferences
) {
    val settingsFlow: Flow<XaraUserSettings> = preferences.settingsFlow
    val gameProfilesFlow: Flow<List<GameProfileEntity>> = dao.observeGameProfiles()
    val boostHistoryFlow: Flow<List<BoostHistoryEntity>> = dao.observeBoostHistory()
    val gameSessionsFlow: Flow<List<GameSessionEntity>> = dao.observeGameSessions()
    val touchProfilesFlow: Flow<List<TouchProfileEntity>> = dao.observeTouchProfiles()
    val macroConfigsFlow: Flow<List<MacroConfigEntity>> = dao.observeMacroConfigs()

    suspend fun seedInitialPresetsIfEmpty() {
        val currentTouch = dao.observeTouchProfiles().first()
        if (currentTouch.isEmpty()) {
            dao.upsertTouchProfile(
                TouchProfileEntity(
                    name = "Precision Sniper",
                    gamePackage = null,
                    sensitivityX = 0.85f,
                    sensitivityY = 0.75f,
                    touchAreaScale = 0.90f,
                    touchZoneMode = TouchZoneMode.RIGHT_AIM.name,
                    isPreset = true
                )
            )
            dao.upsertTouchProfile(
                TouchProfileEntity(
                    name = "Claw Rush 4-Finger",
                    gamePackage = null,
                    sensitivityX = 1.35f,
                    sensitivityY = 1.25f,
                    touchAreaScale = 1.15f,
                    touchZoneMode = TouchZoneMode.CLAW_CORNERS.name,
                    isPreset = true
                )
            )
            dao.upsertTouchProfile(
                TouchProfileEntity(
                    name = "Balanced Arena",
                    gamePackage = null,
                    sensitivityX = 1.0f,
                    sensitivityY = 1.0f,
                    touchAreaScale = 1.0f,
                    touchZoneMode = TouchZoneMode.FULL_SCREEN.name,
                    isPreset = true
                )
            )
        }
    }

    suspend fun upsertGameProfile(profile: GameProfileEntity) = dao.upsertGameProfile(profile)
    suspend fun getGameProfile(pkg: String): GameProfileEntity? = dao.getGameProfile(pkg)

    suspend fun recordBoost(item: BoostHistoryEntity) = dao.insertBoostHistory(item)
    suspend fun recordSession(session: GameSessionEntity) = dao.insertGameSession(session)

    suspend fun saveTouchProfile(profile: TouchProfileEntity) = dao.upsertTouchProfile(profile)
    suspend fun deleteTouchProfile(id: Long) = dao.deleteTouchProfile(id)

    suspend fun saveMacroConfig(config: MacroConfigEntity) = dao.upsertMacroConfig(config)
    suspend fun deleteMacroConfig(id: Long) = dao.deleteMacroConfig(id)

    suspend fun setGamerTag(tag: String) = preferences.setGamerTag(tag)
    suspend fun setBoostMode(mode: BoostMode) = preferences.setBoostMode(mode)
    suspend fun setThemeAccent(accent: ThemeAccent) = preferences.setThemeAccent(accent)
    suspend fun setAnimationsEnabled(enabled: Boolean) = preferences.setAnimationsEnabled(enabled)
    suspend fun setNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)
    suspend fun setAutoCleanupOnBoost(enabled: Boolean) = preferences.setAutoCleanupOnBoost(enabled)
    suspend fun setActiveGameDetection(enabled: Boolean) = preferences.setActiveGameDetection(enabled)
    suspend fun setAutoFloatingOnGameLaunch(enabled: Boolean) = preferences.setAutoFloatingOnGameLaunch(enabled)
    suspend fun setFloatingSideRight(rightSide: Boolean) = preferences.setFloatingSideRight(rightSide)
    suspend fun setFloatingMiniHudEnabled(enabled: Boolean) = preferences.setFloatingMiniHudEnabled(enabled)
    suspend fun setFloatingTransparentMode(transparent: Boolean) = preferences.setFloatingTransparentMode(transparent)
    suspend fun setFloatingPositionPreset(preset: FloatingPositionPreset) = preferences.setFloatingPositionPreset(preset)
    suspend fun setGraphicsSettings(
        driver: GraphicsDriverOption,
        profile: String,
        textureMode: TextureOptMode
    ) = preferences.setGraphicsSettings(driver, profile, textureMode)

    suspend fun resetAllSettings() {
        preferences.resetAll()
        dao.clearGameProfiles()
        dao.clearBoostHistory()
        dao.clearGameSessions()
        dao.clearCustomTouchProfiles()
        dao.clearMacroConfigs()
    }
}
