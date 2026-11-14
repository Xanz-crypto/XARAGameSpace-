package com.xara.gamespace.engine

import com.xara.gamespace.data.repository.XaraRepository
import com.xara.gamespace.domain.model.CompilationResult
import com.xara.gamespace.domain.model.CompileMode
import com.xara.gamespace.domain.model.InstalledGameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameCompilerEngine(
    private val shizukuEngine: ShizukuEngine,
    private val repository: XaraRepository
) {
    suspend fun compilePackage(
        game: InstalledGameItem,
        mode: CompileMode
    ): CompilationResult = withContext(Dispatchers.IO) {
        val shizuku = shizukuEngine.status.value
        if (!shizuku.isAppInstalled || !shizuku.isBinderAlive || !shizuku.isPermissionGranted) {
            val reason = when {
                !shizuku.isAppInstalled -> "Shizuku is not installed. Install Shizuku and enable Wireless Debugging to execute ART package compilation."
                !shizuku.isBinderAlive -> "Shizuku service is not running. Start Shizuku via Wireless Debugging and reconnect."
                else -> "Shizuku permission is not granted for XARAGameSpace."
            }
            return@withContext CompilationResult(
                packageName = game.packageName,
                appName = game.appName,
                mode = mode,
                success = false,
                outputMessage = "Compilation skipped — elevated shell access required.",
                errorDetails = reason
            )
        }

        val safePkg = game.packageName.replace(Regex("[^a-zA-Z0-9._]"), "")
        val command = "cmd package compile -m ${mode.flag} -f $safePkg"
        val (ok, output) = shizukuEngine.executeCommand(command)

        val now = System.currentTimeMillis()
        if (ok) {
            val existing = repository.getGameProfile(game.packageName)
            if (existing != null) {
                repository.upsertGameProfile(
                    existing.copy(
                        compileStatus = "Compiled (${mode.flag})",
                        lastCompiledTimestamp = now
                    )
                )
            } else {
                repository.upsertGameProfile(
                    com.xara.gamespace.data.local.GameProfileEntity(
                        packageName = game.packageName,
                        appName = game.appName,
                        isAddedToSpace = game.isAddedToSpace,
                        compileStatus = "Compiled (${mode.flag})",
                        lastCompiledTimestamp = now
                    )
                )
            }
        }

        CompilationResult(
            packageName = game.packageName,
            appName = game.appName,
            mode = mode,
            success = ok,
            outputMessage = if (ok) "ART compilation succeeded (${mode.flag})" else "Compilation returned an error",
            errorDetails = if (ok) output.ifBlank { "Success" } else output,
            timestamp = now
        )
    }
}
