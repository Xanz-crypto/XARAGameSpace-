package com.xara.gamespace.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.xara.gamespace.ui.components.XaraLogoCrest
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val xaraColors = LocalXaraColors.current
    val scaleAnim = remember { Animatable(0.72f) }
    val alphaAnim = remember { Animatable(0f) }
    val sweepAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        sweepAnim.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        delay(220)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        xaraColors.accentSecondary.copy(alpha = 0.22f),
                        XaraObsidian
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(xaraColors.accentPrimary, xaraColors.accentSecondary, xaraColors.accentPrimary)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * sweepAnim.value,
                        useCenter = false,
                        topLeft = Offset(stroke, stroke),
                        size = Size(size.width - stroke * 2, size.height - stroke * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                XaraLogoCrest(size = 96.dp)
            }

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = XaraTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.labelLarge,
                color = xaraColors.accentPrimary
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                progress = { sweepAnim.value },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = xaraColors.accentPrimary,
                trackColor = xaraColors.glassBorder
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "POWERED BY ${stringResource(R.string.brand_name).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = XaraTextSecondary
            )
        }
    }
}
