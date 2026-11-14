package com.xara.gamespace.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xara.gamespace.ui.components.XaraBottomBar
import com.xara.gamespace.ui.components.XaraCosmicBackdrop
import com.xara.gamespace.ui.components.XaraMainTab
import com.xara.gamespace.ui.components.XaraSideNavigationRail
import com.xara.gamespace.ui.navigation.BatteryEngineRoute
import com.xara.gamespace.ui.navigation.BoostRoute
import com.xara.gamespace.ui.navigation.CpuMonitorRoute
import com.xara.gamespace.ui.navigation.DeviceInfoRoute
import com.xara.gamespace.ui.navigation.FloatingPanelRoute
import com.xara.gamespace.ui.navigation.GameCompilerRoute
import com.xara.gamespace.ui.navigation.GameDetailRoute
import com.xara.gamespace.ui.navigation.GamesRoute
import com.xara.gamespace.ui.navigation.GamingSessionRoute
import com.xara.gamespace.ui.navigation.GraphicsEngineRoute
import com.xara.gamespace.ui.navigation.HomeRoute
import com.xara.gamespace.ui.navigation.MacroConfigRoute
import com.xara.gamespace.ui.navigation.NetworkMonitorRoute
import com.xara.gamespace.ui.navigation.ProfileRoute
import com.xara.gamespace.ui.navigation.RamMonitorRoute
import com.xara.gamespace.ui.navigation.RealTimeHudRoute
import com.xara.gamespace.ui.navigation.ShizukuManagerRoute
import com.xara.gamespace.ui.navigation.ToolsRoute
import com.xara.gamespace.ui.navigation.TouchOptimizerRoute
import com.xara.gamespace.ui.screens.BatteryEngineScreen
import com.xara.gamespace.ui.screens.BoostEngineScreen
import com.xara.gamespace.ui.screens.CpuMonitorScreen
import com.xara.gamespace.ui.screens.DeviceInfoScreen
import com.xara.gamespace.ui.screens.FloatingPanelScreen
import com.xara.gamespace.ui.screens.GameCompilerScreen
import com.xara.gamespace.ui.screens.GameDetailScreen
import com.xara.gamespace.ui.screens.GamesManagerScreen
import com.xara.gamespace.ui.screens.GamingSessionScreen
import com.xara.gamespace.ui.screens.GraphicsEngineScreen
import com.xara.gamespace.ui.screens.HomeDashboardScreen
import com.xara.gamespace.ui.screens.MacroConfigScreen
import com.xara.gamespace.ui.screens.NetworkMonitorScreen
import com.xara.gamespace.ui.screens.ProfileSettingsScreen
import com.xara.gamespace.ui.screens.RamMonitorScreen
import com.xara.gamespace.ui.screens.RealTimeHudScreen
import com.xara.gamespace.ui.screens.ShizukuManagerScreen
import com.xara.gamespace.ui.screens.SplashScreen
import com.xara.gamespace.ui.screens.ToolsCenterScreen
import com.xara.gamespace.ui.screens.TouchOptimizerScreen
import com.xara.gamespace.ui.theme.XaraGameSpaceTheme

@Composable
fun XaraAppRoot(
    viewModel: XaraMainViewModel = viewModel()
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val pendingDeepLink by viewModel.pendingDeepLinkRoute.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    XaraGameSpaceTheme(themeAccent = userSettings.themeAccent) {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val destination = navBackStackEntry?.destination
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(statusBanner) {
                statusBanner?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearStatusBanner()
                }
            }

            LaunchedEffect(pendingDeepLink) {
                when (pendingDeepLink) {
                    "graphics" -> {
                        navController.navigate(GraphicsEngineRoute)
                        viewModel.consumeDeepLinkRoute()
                    }
                    "touch" -> {
                        navController.navigate(TouchOptimizerRoute)
                        viewModel.consumeDeepLinkRoute()
                    }
                    "macro" -> {
                        navController.navigate(MacroConfigRoute)
                        viewModel.consumeDeepLinkRoute()
                    }
                }
            }

            val currentTab: XaraMainTab? = when {
                destination?.hasRoute<HomeRoute>() == true -> XaraMainTab.HOME
                destination?.hasRoute<GamesRoute>() == true -> XaraMainTab.GAMES
                destination?.hasRoute<BoostRoute>() == true -> XaraMainTab.BOOST
                destination?.hasRoute<ToolsRoute>() == true -> XaraMainTab.TOOLS
                destination?.hasRoute<ProfileRoute>() == true -> XaraMainTab.PROFILE
                else -> null
            }

            val navigateToTab: (XaraMainTab) -> Unit = { tab ->
                val target: Any = when (tab) {
                    XaraMainTab.HOME -> HomeRoute
                    XaraMainTab.GAMES -> GamesRoute
                    XaraMainTab.BOOST -> BoostRoute
                    XaraMainTab.TOOLS -> ToolsRoute
                    XaraMainTab.PROFILE -> ProfileRoute
                }
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useSideRail = maxWidth >= 600.dp

                XaraCosmicBackdrop {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useSideRail && currentTab != null) {
                            XaraSideNavigationRail(
                                currentTab = currentTab,
                                onSelectTab = navigateToTab
                            )
                        }

                        Scaffold(
                            containerColor = Color.Transparent,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            bottomBar = {
                                if (!useSideRail && currentTab != null) {
                                    XaraBottomBar(
                                        currentTab = currentTab,
                                        onSelectTab = navigateToTab
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (useSideRail) 0.dp else innerPadding.calculateBottomPadding())
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = HomeRoute,
                                    enterTransition = {
                                        fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(220)
                                        )
                                    },
                                    exitTransition = {
                                        fadeOut(animationSpec = tween(180))
                                    },
                                    popEnterTransition = {
                                        fadeIn(animationSpec = tween(220))
                                    },
                                    popExitTransition = {
                                        fadeOut(animationSpec = tween(180)) + slideOutOfContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(220)
                                        )
                                    }
                                ) {
                                    composable<HomeRoute> {
                                        HomeDashboardScreen(
                                            viewModel = viewModel,
                                            onOpenShizuku = { navController.navigate(ShizukuManagerRoute) },
                                            onOpenRam = { navController.navigate(RamMonitorRoute) },
                                            onOpenCpu = { navController.navigate(CpuMonitorRoute) },
                                            onOpenBattery = { navController.navigate(BatteryEngineRoute) },
                                            onOpenNetwork = { navController.navigate(NetworkMonitorRoute) },
                                            onOpenHud = { navController.navigate(RealTimeHudRoute) },
                                            onOpenGraphics = { navController.navigate(GraphicsEngineRoute) },
                                            onOpenTouch = { navController.navigate(TouchOptimizerRoute) },
                                            onOpenMacro = { navController.navigate(MacroConfigRoute) },
                                            onOpenFloatingPanel = { navController.navigate(FloatingPanelRoute) },
                                            onOpenCompiler = { navController.navigate(GameCompilerRoute) },
                                            onOpenSession = { navController.navigate(GamingSessionRoute) },
                                            onOpenGamesTab = { navigateToTab(XaraMainTab.GAMES) },
                                            onOpenGameDetail = { pkg -> navController.navigate(GameDetailRoute(pkg)) }
                                        )
                                    }

                                    composable<GamesRoute> {
                                        GamesManagerScreen(
                                            viewModel = viewModel,
                                            onOpenGameDetail = { pkg -> navController.navigate(GameDetailRoute(pkg)) }
                                        )
                                    }

                                    composable<BoostRoute> {
                                        BoostEngineScreen(viewModel = viewModel)
                                    }

                                    composable<ToolsRoute> {
                                        ToolsCenterScreen(
                                            viewModel = viewModel,
                                            onOpenRam = { navController.navigate(RamMonitorRoute) },
                                            onOpenCpu = { navController.navigate(CpuMonitorRoute) },
                                            onOpenNetwork = { navController.navigate(NetworkMonitorRoute) },
                                            onOpenGraphics = { navController.navigate(GraphicsEngineRoute) },
                                            onOpenTouch = { navController.navigate(TouchOptimizerRoute) },
                                            onOpenMacro = { navController.navigate(MacroConfigRoute) },
                                            onOpenFloatingPanel = { navController.navigate(FloatingPanelRoute) },
                                            onOpenRealTimeHud = { navController.navigate(RealTimeHudRoute) },
                                            onOpenShizuku = { navController.navigate(ShizukuManagerRoute) },
                                            onOpenCompiler = { navController.navigate(GameCompilerRoute) },
                                            onOpenBattery = { navController.navigate(BatteryEngineRoute) },
                                            onOpenSession = { navController.navigate(GamingSessionRoute) },
                                            onOpenDeviceInfo = { navController.navigate(DeviceInfoRoute) }
                                        )
                                    }

                                    composable<ProfileRoute> {
                                        ProfileSettingsScreen(
                                            viewModel = viewModel,
                                            onOpenShizuku = { navController.navigate(ShizukuManagerRoute) },
                                            onOpenDeviceInfo = { navController.navigate(DeviceInfoRoute) }
                                        )
                                    }

                                    composable<GameDetailRoute> { backStackEntry ->
                                        val route = backStackEntry.toRoute<GameDetailRoute>()
                                        GameDetailScreen(
                                            packageName = route.packageName,
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable<RamMonitorRoute> {
                                        RamMonitorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<CpuMonitorRoute> {
                                        CpuMonitorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<NetworkMonitorRoute> {
                                        NetworkMonitorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<GraphicsEngineRoute> {
                                        GraphicsEngineScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<TouchOptimizerRoute> {
                                        TouchOptimizerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<MacroConfigRoute> {
                                        MacroConfigScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<FloatingPanelRoute> {
                                        FloatingPanelScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<RealTimeHudRoute> {
                                        RealTimeHudScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<ShizukuManagerRoute> {
                                        ShizukuManagerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<GameCompilerRoute> {
                                        GameCompilerScreen(
                                            viewModel = viewModel,
                                            onOpenShizuku = { navController.navigate(ShizukuManagerRoute) },
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable<BatteryEngineRoute> {
                                        BatteryEngineScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<GamingSessionRoute> {
                                        GamingSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                    composable<DeviceInfoRoute> {
                                        DeviceInfoScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
