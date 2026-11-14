package com.xara.gamespace.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object GamesRoute
@Serializable object BoostRoute
@Serializable object ToolsRoute
@Serializable object ProfileRoute

@Serializable data class GameDetailRoute(val packageName: String)

@Serializable object RamMonitorRoute
@Serializable object CpuMonitorRoute
@Serializable object NetworkMonitorRoute
@Serializable object GraphicsEngineRoute
@Serializable object TouchOptimizerRoute
@Serializable object MacroConfigRoute
@Serializable object FloatingPanelRoute
@Serializable object RealTimeHudRoute
@Serializable object ShizukuManagerRoute
@Serializable object GameCompilerRoute
@Serializable object BatteryEngineRoute
@Serializable object GamingSessionRoute
@Serializable object DeviceInfoRoute
