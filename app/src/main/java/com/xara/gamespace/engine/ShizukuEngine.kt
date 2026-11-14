package com.xara.gamespace.engine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import com.xara.gamespace.domain.model.ShizukuStatus
import com.xara.gamespace.shizuku.IXaraShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShizukuEngine(private val context: Context) {

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE_SHIZUKU = 9001
    }

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var shellService: IXaraShellService? = null
    private var isManualDisconnected = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, XaraShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("xara_shell")
            .debuggable(false)
            .version(1)
    }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shellService = IXaraShellService.Stub.asInterface(service)
            refreshStatus(lastError = null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            refreshStatus(lastError = "Shizuku UserService disconnected")
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (!isManualDisconnected) {
            refreshStatus(lastError = null)
            if (isPermissionGrantedInternal()) {
                bindUserServiceSafe()
            }
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        refreshStatus(lastError = "Shizuku binder dead (service stopped)")
    }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    isManualDisconnected = false
                    bindUserServiceSafe()
                }
                refreshStatus(
                    lastError = if (granted) null else "Shizuku permission denied by user"
                )
            }
        }

    fun initialize() {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        }
        refreshStatus()
    }

    fun destroy() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            unbindUserServiceSafe()
        }
    }

    fun refreshStatus(lastError: String? = _status.value.lastError) {
        val installed = isShizukuAppInstalled()
        val binderAlive = if (!isManualDisconnected) {
            runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        } else {
            false
        }
        val granted = if (binderAlive) isPermissionGrantedInternal() else false
        val version = if (binderAlive) runCatching { Shizuku.getVersion() }.getOrNull() else null
        val uid = if (binderAlive) runCatching { Shizuku.getUid() }.getOrNull() else null
        val bound = shellService != null && binderAlive && granted

        val privilegeLabel = when {
            isManualDisconnected -> "Disconnected (Manual)"
            !installed -> "Shizuku App Not Installed"
            !binderAlive -> "Shizuku Service Not Running"
            !granted -> "Awaiting Permission"
            uid == 0 -> "Connected (Root UID 0)"
            uid == 2000 -> "Connected (ADB Shell UID 2000)"
            bound -> "Connected & Authorized"
            else -> "Binder Active (Binding Service)"
        }

        _status.value = ShizukuStatus(
            isAppInstalled = installed,
            isBinderAlive = binderAlive,
            isPermissionGranted = granted,
            isUserServiceBound = bound,
            shizukuVersion = version,
            uid = uid,
            privilegeLabel = privilegeLabel,
            lastError = lastError
        )
    }

    fun connectOrRequestPermission() {
        isManualDisconnected = false
        val installed = isShizukuAppInstalled()
        if (!installed) {
            refreshStatus("Shizuku app is not installed on this device.")
            return
        }
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!alive) {
            refreshStatus("Shizuku binder is offline. Start Shizuku via Wireless Debugging first.")
            return
        }
        val granted = isPermissionGrantedInternal()
        if (!granted) {
            runCatching { Shizuku.requestPermission(REQUEST_CODE_SHIZUKU) }
                .onFailure { err ->
                    refreshStatus("Failed to request permission: ${err.message}")
                }
        } else {
            bindUserServiceSafe()
            refreshStatus(null)
        }
    }

    fun reconnect() {
        isManualDisconnected = false
        unbindUserServiceSafe()
        connectOrRequestPermission()
    }

    fun disconnect() {
        isManualDisconnected = true
        unbindUserServiceSafe()
        refreshStatus(null)
    }

    fun openShizukuApp(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
        }
        val playIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://shizuku.rikka.app/")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(playIntent)
            true
        }.getOrDefault(false)
    }

    suspend fun executeCommand(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val service = shellService
        if (service == null || !status.value.isBinderAlive || !status.value.isPermissionGranted) {
            return@withContext false to "Shizuku shell service is not connected or authorized."
        }
        return@withContext runCatching {
            val raw = service.runCommand(command)
            val parts = raw.split("|", limit = 2)
            val exitCode = parts.firstOrNull()?.removePrefix("EXIT:")?.toIntOrNull() ?: -1
            val output = parts.getOrNull(1).orEmpty()
            (exitCode == 0) to output
        }.getOrElse { err ->
            false to "Shizuku IPC failed: ${err.message ?: err.javaClass.simpleName}"
        }
    }

    private fun isShizukuAppInstalled(): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    private fun isPermissionGrantedInternal(): Boolean {
        return runCatching {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    private fun bindUserServiceSafe() {
        runCatching {
            if (!Shizuku.isPreV11() && Shizuku.pingBinder()) {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        }.onFailure { err ->
            refreshStatus("UserService bind error: ${err.message}")
        }
    }

    private fun unbindUserServiceSafe() {
        runCatching {
            if (shellService != null) {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        }
        shellService = null
    }
}
