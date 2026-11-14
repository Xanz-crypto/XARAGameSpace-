package com.xara.gamespace.engine

import android.os.Process
import com.xara.gamespace.shizuku.IXaraShellService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class XaraShellUserService : IXaraShellService.Stub() {

    override fun runCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            val combined = buildString {
                if (stdout.isNotBlank()) append(stdout.trim())
                if (stderr.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("ERR: ").append(stderr.trim())
                }
                if (isEmpty()) append("Exit code: $exitCode")
            }
            "EXIT:$exitCode|$combined"
        } catch (t: Throwable) {
            "EXIT:-1|Exception: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    override fun getServerUid(): Int = Process.myUid()

    override fun destroy() {
        exitProcess(0)
    }
}
