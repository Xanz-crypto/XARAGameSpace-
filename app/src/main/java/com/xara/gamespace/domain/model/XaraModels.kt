package com.xara.gamespace.domain.model

enum class BoostMode(
    val displayName: String,
    val shortLabel: String,
    val subtitle: String
) {
    PERFORMANCE(
        displayName = "Performance Mode",
        shortLabel = "PERF",
        subtitle = "Pre-game RAM inspection, background process cleanup & live thermal telemetry"
    ),
    BALANCED(
        displayName = "Balanced Mode",
        shortLabel = "BALANCED",
        subtitle = "Optimal stability balancing system responsiveness and battery efficiency"
    ),
    BATTERY_SAVER(
        displayName = "Battery Saver Mode",
        shortLabel = "ECO",
        subtitle = "Extends marathon gaming stamina with safe power-saving recommendations"
    )
}

enum class ThemeAccent(val displayName: String) {
    CYBER_CYAN("Cyber Cyan"),
    VIOLET_PULSE("Violet Pulse"),
    ELECTRIC_BLUE("Electric Blue")
}

enum class GraphicsDriverOption(
    val displayName: String,
    val settingValue: String,
    val description: String
) {
    DEFAULT("Default Driver", "0", "Standard Android GPU driver assigned by system"),
    SYSTEM("System Driver", "3", "Force built-in platform graphics driver"),
    ANGLE("ANGLE Driver", "angle", "Vulkan-backed OpenGL ES translation layer (if device supports ANGLE)"),
    GAME_DRIVER("Game Driver", "1", "Updatable OEM Game Driver (requires device & Shizuku/Developer Options)")
}

enum class TextureOptMode(
    val displayName: String,
    val resolutionScalePercent: Int,
    val description: String
) {
    SHARP_NATIVE("Sharp Native", 100, "Full native resolution rendering profile"),
    BALANCED("Balanced Filter", 85, "Moderate texture filtering profile for stable frametimes"),
    BURIK_SOFT("Burik Soft", 70, "Lower-resolution soft texture profile for reduced GPU bandwidth load"),
    BURIK_PIXEL("Burik Pixel", 55, "Ultra-low pixel texture profile focused on thermal headroom")
}

enum class TouchZoneMode(val displayName: String) {
    FULL_SCREEN("Full Screen"),
    RIGHT_AIM("Right Aim Zone"),
    LEFT_MOVE("Left Joystick Zone"),
    CLAW_CORNERS("4-Finger Claw Zones")
}

enum class FloatingPositionPreset(val displayName: String) {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    CENTER_LEFT("Center Left"),
    CUSTOM("Custom Drag")
}

enum class CompileMode(val flag: String, val label: String, val description: String) {
    SPEED_PROFILE("speed-profile", "Speed Profile (Recommended)", "AOT-compiles hot methods using collected ART profiles"),
    SPEED("speed", "Full Speed AOT", "Compiles all methods for faster startup and execution"),
    QUICKEN("quicken", "Quicken", "Lightweight dex optimization for rapid verification"),
    VERIFY("verify", "Verify Only", "Fast bytecode verification without full native compilation")
}

data class DeviceTelemetry(
    val totalRamMb: Long = 0L,
    val usedRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val ramUsagePercent: Int = 0,
    val isLowMemory: Boolean = false,
    val cpuUsagePercent: Float? = null,
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val cpuCurrentFreqMhz: Int? = null,
    val cpuMaxFreqMhz: Int? = null,
    val cpuCoreFreqsMhz: List<Int?> = emptyList(),
    val cpuCoreMaxFreqsMhz: List<Int?> = emptyList(),
    val cpuArchitecture: String = "",
    val batteryPercent: Int = 0,
    val batteryTempCelsius: Float = 0f,
    val batteryVoltageMv: Int = 0,
    val batteryHealth: String = "Unknown",
    val chargingStatus: String = "Unknown",
    val pluggedSource: String = "Battery",
    val isPowerSaveMode: Boolean = false,
    val refreshRateHz: Float = 60f,
    val supportedRefreshRates: List<Float> = listOf(60f),
    val hudFps: Float? = null,
    val hudFrameTimeMs: Float? = null,
    val networkLatencyMs: Int? = null,
    val networkType: String = "Checking...",
    val deviceModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val socHardware: String = "",
    val boardName: String = "",
    val securityPatch: String = "",
    val screenResolution: String = "",
    val screenDpi: Int = 0,
    val performanceScore: Int = 0,
    val performanceScoreBreakdown: String = ""
)

data class InstalledGameItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemGame: Boolean,
    val isAddedToSpace: Boolean,
    val isFavorite: Boolean,
    val lastPlayedTimestamp: Long,
    val totalPlaytimeSeconds: Long,
    val boostModeOverride: BoostMode?,
    val graphicsDriver: GraphicsDriverOption,
    val textureOptMode: TextureOptMode,
    val sensitivityX: Float,
    val sensitivityY: Float,
    val compileStatus: String,
    val lastCompiledTimestamp: Long
)

data class BoostResult(
    val mode: BoostMode,
    val beforeUsedRamMb: Long,
    val afterUsedRamMb: Long,
    val totalRamMb: Long,
    val freedRamMb: Long,
    val runningProcessesInspected: Int,
    val backgroundProcessesCleared: Int,
    val recommendations: List<String>,
    val durationMs: Long,
    val timestamp: Long
)

data class ShizukuStatus(
    val isAppInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isUserServiceBound: Boolean = false,
    val shizukuVersion: Int? = null,
    val uid: Int? = null,
    val privilegeLabel: String = "Disconnected",
    val lastError: String? = null
)

data class CompilationResult(
    val packageName: String,
    val appName: String,
    val mode: CompileMode,
    val success: Boolean,
    val outputMessage: String,
    val errorDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveSessionState(
    val isRunning: Boolean = false,
    val activeGamePackage: String? = null,
    val activeGameName: String = "Manual Gaming Session",
    val elapsedSeconds: Long = 0L,
    val startTimestamp: Long = 0L,
    val initialTempC: Float = 0f,
    val peakTempC: Float = 0f,
    val initialBatteryPct: Int = 0,
    val avgRamPercent: Int = 0,
    val appliedMode: BoostMode = BoostMode.BALANCED,
    val tempSettingsApplied: Boolean = false
)

data class GraphicsCompatibility(
    val vulkanSupported: Boolean,
    val vulkanVersion: String,
    val openGlEsVersion: String,
    val angleAvailable: Boolean,
    val updatableDriverSupported: Boolean,
    val shizukuRequiredNote: String
)

data class XaraUserSettings(
    val gamerTag: String = "XARA_PLAYER",
    val boostMode: BoostMode = BoostMode.BALANCED,
    val themeAccent: ThemeAccent = ThemeAccent.CYBER_CYAN,
    val animationsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoCleanupOnBoost: Boolean = true,
    val activeGameDetectionEnabled: Boolean = true,
    val autoFloatingPanelOnGameLaunch: Boolean = true,
    val floatingSideRight: Boolean = true,
    val floatingMiniHudEnabled: Boolean = true,
    val floatingTransparentMode: Boolean = false,
    val floatingPositionPreset: FloatingPositionPreset = FloatingPositionPreset.TOP_RIGHT,
    val selectedGraphicsDriver: GraphicsDriverOption = GraphicsDriverOption.DEFAULT,
    val selectedGraphicsProfile: String = "Competitive",
    val selectedTextureMode: TextureOptMode = TextureOptMode.SHARP_NATIVE
)
