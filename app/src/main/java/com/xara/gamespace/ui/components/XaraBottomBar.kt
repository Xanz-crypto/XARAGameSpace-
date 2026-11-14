package com.xara.gamespace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraGraphiteDark
import com.xara.gamespace.ui.theme.XaraTextSecondary

enum class XaraMainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTagName: String
) {
    HOME("HOME", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    GAMES("GAMES", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "nav_games"),
    BOOST("BOOST", Icons.Filled.Bolt, Icons.Outlined.Bolt, "nav_boost"),
    TOOLS("TOOLS", Icons.Filled.Build, Icons.Outlined.Build, "nav_tools"),
    PROFILE("PROFILE", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun XaraBottomBar(
    currentTab: XaraMainTab,
    onSelectTab: (XaraMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val xaraColors = LocalXaraColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        XaraGraphiteDark.copy(alpha = 0.88f),
                        XaraGraphiteDark.copy(alpha = 0.98f)
                    )
                )
            )
            .border(width = 1.dp, color = xaraColors.glassBorder.copy(alpha = 0.7f))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            XaraMainTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.06f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_scale"
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) xaraColors.accentPrimary else XaraTextSecondary,
                    label = "tab_tint"
                )
                val pillBg = if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            xaraColors.accentPrimary.copy(alpha = 0.20f),
                            xaraColors.accentSecondary.copy(alpha = 0.20f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pillBg)
                        .minimumInteractiveComponentSize()
                        .testTag(tab.testTagName)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun XaraSideNavigationRail(
    currentTab: XaraMainTab,
    onSelectTab: (XaraMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val xaraColors = LocalXaraColors.current
    NavigationRail(
        modifier = modifier,
        containerColor = XaraGraphiteDark,
        header = {
            XaraLogoCrest(size = 38.dp, modifier = Modifier.padding(vertical = 12.dp))
        }
    ) {
        XaraMainTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = xaraColors.accentPrimary,
                    selectedTextColor = xaraColors.accentPrimary,
                    indicatorColor = xaraColors.accentPrimary.copy(alpha = 0.16f),
                    unselectedIconColor = XaraTextSecondary,
                    unselectedTextColor = XaraTextSecondary
                ),
                modifier = Modifier.testTag(tab.testTagName)
            )
        }
    }
}
