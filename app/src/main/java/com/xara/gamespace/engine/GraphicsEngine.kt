package com.xara.gamespace.engine

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.xara.gamespace.domain.model.GraphicsCompatibility
import com.xara.gamespace.domain.model.GraphicsDriverOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GraphicsEngine(
    private val context: Context,
    private val shizukuEngine: ShizukuEngine
) {

    fun inspectCompatibility(): GraphicsCompatibility {
        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configInfo = am.deviceConfigurationInfo

        val vulkanSupported = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val vulkanVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && vulkanSupported) {
            val verFeature = pm.systemAvailableFeatures.firstOrNull {
                it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
            }
            if (verFeature != null) {
                val major = (verFeature.version ushr 22) and 0x3FF
                val minor = (verFeature.version ushr 12) and 0x3FF
                "Vulkan $major.$minor"
            } else {
                "Vulkan Supported"
            }
        } else {
            "Not Reported"
        }

        val glEsVersion = configInfo.glEsVersion ?: "3.0"
        val angleAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val updatableDriverSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        return GraphicsCompatibility(
            vulkanSupported = vulkanSupported,
            vulkanVersion = vulkanVersion,
            openGlEsVersion = "OpenGL ES $glEsVersion",
            angleAvailable = angleAvailable,
            updatableDriverSupported = updatableDriverSupported,
            shizukuRequiredNote = "Android protects system graphics driver flags (updatable_driver_all_apps). Without Shizuku or Developer Options, selections are saved as XARA per-game reference profiles."
        )
    }

    suspend fun applyDriverSettingIfPermitted(
        driver: GraphicsDriverOption,
        packageName: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val shizuku = shizukuEngine.status.value
        if (!shizuku.isBinderAlive || !shizuku.isPermissionGranted) {
            return@withContext false to "Saved as XARA Profile only. System driver flags require Shizuku permission or Developer Options."
        }

        val command = when {
            driver == GraphicsDriverOption.ANGLE && packageName != null ->
                "settings put global angle_gl_driver_selection_pkgs $packageName && settings put global angle_gl_driver_selection_values angle"
            driver == GraphicsDriverOption.DEFAULT ->
                "settings put global updatable_driver_all_apps 0"
            driver == GraphicsDriverOption.SYSTEM ->
                "settings put global updatable_driver_all_apps 3"
            driver == GraphicsDriverOption.GAME_DRIVER ->
                "settings put global updatable_driver_all_apps 1"
            else ->
                "settings get global updatable_driver_all_apps"
        }

        val (ok, output) = shizukuEngine.executeCommand(command)
        if (ok) {
            true to "Applied ${driver.displayName} flag via Shizuku Shell."
        } else {
            false to "System denied flag modification (${output.take(120)}). Saved as XARA Profile."
        }
    }
}
