package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xara.gamespace.XaraApplication
import com.xara.gamespace.services.FloatingPanelService
import com.xara.gamespace.ui.XaraAppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        handleDeepLink(intent)
        setContent {
            XaraAppRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val route = intent?.getStringExtra(FloatingPanelService.EXTRA_DEEP_LINK_ROUTE) ?: return
        XaraApplication.from(this).container.pendingDeepLinkRoute.value = route
    }
}
