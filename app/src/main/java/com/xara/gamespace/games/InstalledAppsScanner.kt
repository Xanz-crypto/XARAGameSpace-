package com.xara.gamespace.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.xara.gamespace.data.local.GameProfileEntity
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.InstalledGameItem
import com.xara.gamespace.domain.model.TextureOptMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class InstalledAppsScanner(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, Bitmap>()

    suspend fun scanInstalledApps(
        savedProfiles: Map<String, GameProfileEntity>
    ): List<InstalledGameItem> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seenPackages = HashSet<String>()
        val results = mutableListOf<InstalledGameItem>()

        for (info in resolveInfos) {
            val appInfo = info.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue

            val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val versionName = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L)).versionName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0).versionName
                }
            }.getOrNull() ?: "1.0"

            val isSystemDetectedGame = detectIsGame(appInfo, pkg)
            val saved = savedProfiles[pkg]

            results += InstalledGameItem(
                packageName = pkg,
                appName = label,
                versionName = versionName,
                isSystemGame = isSystemDetectedGame,
                isAddedToSpace = saved?.isAddedToSpace ?: isSystemDetectedGame,
                isFavorite = saved?.isFavorite ?: false,
                lastPlayedTimestamp = saved?.lastPlayedTimestamp ?: 0L,
                totalPlaytimeSeconds = saved?.totalPlaytimeSeconds ?: 0L,
                boostModeOverride = saved?.boostModeOverride?.let {
                    runCatching { BoostMode.valueOf(it) }.getOrNull()
                },
                graphicsDriver = saved?.graphicsDriver?.let {
                    runCatching { GraphicsDriverOption.valueOf(it) }.getOrNull()
                } ?: GraphicsDriverOption.DEFAULT,
                textureOptMode = saved?.textureOptMode?.let {
                    runCatching { TextureOptMode.valueOf(it) }.getOrNull()
                } ?: TextureOptMode.SHARP_NATIVE,
                sensitivityX = saved?.sensitivityX ?: 1.0f,
                sensitivityY = saved?.sensitivityY ?: 1.0f,
                compileStatus = saved?.compileStatus ?: "Not Compiled",
                lastCompiledTimestamp = saved?.lastCompiledTimestamp ?: 0L
            )
        }

        results.sortedWith(
            compareByDescending<InstalledGameItem> { it.isFavorite }
                .thenByDescending { it.isAddedToSpace }
                .thenByDescending { it.lastPlayedTimestamp }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun getAppIconBitmap(packageName: String): Bitmap? {
        iconCache[packageName]?.let { return it }
        val drawable: Drawable = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
            ?: return null
        val bitmap = drawableToBitmap(drawable)
        iconCache[packageName] = bitmap
        return bitmap
    }

    fun launchApp(packageName: String): Boolean {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun detectIsGame(appInfo: ApplicationInfo, pkg: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            appInfo.category == ApplicationInfo.CATEGORY_GAME
        ) {
            return true
        }
        @Suppress("DEPRECATION")
        if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return true
        }
        val lower = pkg.lowercase()
        val knownHints = listOf(
            "pubg", "mobile.legends", "garena", "freefire", "genshin", "honkai",
            "roblox", "minecraft", "codm", "activision", "supercell", "clash",
            "brawlstars", "wildrift", "valorant", "fortnite", "ea.gp", "gameloft",
            "netease", "mihoyo", "hoyoverse", "epicgames", "asphalt", "fifa", "efootball"
        )
        return knownHints.any { lower.contains(it) }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceIn(48, 192)
        val height = drawable.intrinsicHeight.coerceIn(48, 192)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
