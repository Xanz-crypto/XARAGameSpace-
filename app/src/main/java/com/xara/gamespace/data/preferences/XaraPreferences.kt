package com.xara.gamespace.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.FloatingPositionPreset
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.TextureOptMode
import com.xara.gamespace.domain.model.ThemeAccent
import com.xara.gamespace.domain.model.XaraUserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.xaraDataStore by preferencesDataStore(name = "xara_preferences")

class XaraPreferences(private val context: Context) {

    private object Keys {
        val GAMER_TAG = stringPreferencesKey("gamer_tag")
        val BOOST_MODE = stringPreferencesKey("boost_mode")
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_CLEANUP_ON_BOOST = booleanPreferencesKey("auto_cleanup_on_boost")
        val ACTIVE_GAME_DETECTION = booleanPreferencesKey("active_game_detection")
        val AUTO_FLOATING_ON_GAME = booleanPreferencesKey("auto_floating_on_game")
        val FLOATING_SIDE_RIGHT = booleanPreferencesKey("floating_side_right")
        val FLOATING_MINI_HUD = booleanPreferencesKey("floating_mini_hud")
        val FLOATING_TRANSPARENT = booleanPreferencesKey("floating_transparent")
        val FLOATING_POSITION = stringPreferencesKey("floating_position")
        val GRAPHICS_DRIVER = stringPreferencesKey("graphics_driver")
        val GRAPHICS_PROFILE = stringPreferencesKey("graphics_profile")
        val TEXTURE_MODE = stringPreferencesKey("texture_mode")
    }

    val settingsFlow: Flow<XaraUserSettings> = context.xaraDataStore.data.map { prefs ->
        XaraUserSettings(
            gamerTag = prefs[Keys.GAMER_TAG] ?: "XARA_PLAYER",
            boostMode = prefs[Keys.BOOST_MODE]?.let { runCatching { BoostMode.valueOf(it) }.getOrNull() }
                ?: BoostMode.BALANCED,
            themeAccent = prefs[Keys.THEME_ACCENT]?.let { runCatching { ThemeAccent.valueOf(it) }.getOrNull() }
                ?: ThemeAccent.CYBER_CYAN,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            autoCleanupOnBoost = prefs[Keys.AUTO_CLEANUP_ON_BOOST] ?: true,
            activeGameDetectionEnabled = prefs[Keys.ACTIVE_GAME_DETECTION] ?: true,
            autoFloatingPanelOnGameLaunch = prefs[Keys.AUTO_FLOATING_ON_GAME] ?: true,
            floatingSideRight = prefs[Keys.FLOATING_SIDE_RIGHT] ?: true,
            floatingMiniHudEnabled = prefs[Keys.FLOATING_MINI_HUD] ?: true,
            floatingTransparentMode = prefs[Keys.FLOATING_TRANSPARENT] ?: false,
            floatingPositionPreset = prefs[Keys.FLOATING_POSITION]?.let {
                runCatching { FloatingPositionPreset.valueOf(it) }.getOrNull()
            } ?: FloatingPositionPreset.TOP_RIGHT,
            selectedGraphicsDriver = prefs[Keys.GRAPHICS_DRIVER]?.let {
                runCatching { GraphicsDriverOption.valueOf(it) }.getOrNull()
            } ?: GraphicsDriverOption.DEFAULT,
            selectedGraphicsProfile = prefs[Keys.GRAPHICS_PROFILE] ?: "Competitive",
            selectedTextureMode = prefs[Keys.TEXTURE_MODE]?.let {
                runCatching { TextureOptMode.valueOf(it) }.getOrNull()
            } ?: TextureOptMode.SHARP_NATIVE
        )
    }

    suspend fun setGamerTag(tag: String) {
        context.xaraDataStore.edit { it[Keys.GAMER_TAG] = tag.ifBlank { "XARA_PLAYER" } }
    }

    suspend fun setBoostMode(mode: BoostMode) {
        context.xaraDataStore.edit { it[Keys.BOOST_MODE] = mode.name }
    }

    suspend fun setThemeAccent(accent: ThemeAccent) {
        context.xaraDataStore.edit { it[Keys.THEME_ACCENT] = accent.name }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.xaraDataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.xaraDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoCleanupOnBoost(enabled: Boolean) {
        context.xaraDataStore.edit { it[Keys.AUTO_CLEANUP_ON_BOOST] = enabled }
    }

    suspend fun setActiveGameDetection(enabled: Boolean) {
        context.xaraDataStore.edit { it[Keys.ACTIVE_GAME_DETECTION] = enabled }
    }

    suspend fun setAutoFloatingOnGameLaunch(enabled: Boolean) {
        context.xaraDataStore.edit { it[Keys.AUTO_FLOATING_ON_GAME] = enabled }
    }

    suspend fun setFloatingSideRight(rightSide: Boolean) {
        context.xaraDataStore.edit { it[Keys.FLOATING_SIDE_RIGHT] = rightSide }
    }

    suspend fun setFloatingMiniHudEnabled(enabled: Boolean) {
        context.xaraDataStore.edit { it[Keys.FLOATING_MINI_HUD] = enabled }
    }

    suspend fun setFloatingTransparentMode(transparent: Boolean) {
        context.xaraDataStore.edit { it[Keys.FLOATING_TRANSPARENT] = transparent }
    }

    suspend fun setFloatingPositionPreset(preset: FloatingPositionPreset) {
        context.xaraDataStore.edit { it[Keys.FLOATING_POSITION] = preset.name }
    }

    suspend fun setGraphicsSettings(
        driver: GraphicsDriverOption,
        profile: String,
        textureMode: TextureOptMode
    ) {
        context.xaraDataStore.edit {
            it[Keys.GRAPHICS_DRIVER] = driver.name
            it[Keys.GRAPHICS_PROFILE] = profile
            it[Keys.TEXTURE_MODE] = textureMode.name
        }
    }

    suspend fun resetAll() {
        context.xaraDataStore.edit { it.clear() }
    }
}
