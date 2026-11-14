package com.xara.gamespace.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary

@Composable
fun PerformanceGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    label: String = "SYSTEM SCORE"
) {
    val xaraColors = LocalXaraColors.current
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "perf_score_anim"
    )

    val scoreColor = when {
        score >= 78 -> xaraColors.accentPrimary
        score >= 55 -> xaraColors.thermalCool
        score >= 35 -> xaraColors.thermalWarm
        else -> xaraColors.thermalHot
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 10.dp.toPx()
            val inset = strokeW / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Track
            drawArc(
                color = xaraColors.glassBorder.copy(alpha = 0.45f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            // Active Sweep
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        xaraColors.accentSecondary,
                        scoreColor,
                        xaraColors.accentPrimary
                    )
                ),
                startAngle = 135f,
                sweepAngle = 270f * (animatedScore / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                color = scoreColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = XaraTextSecondary
            )
        }
    }
}

@Composable
fun BoostCoreButton(
    isBoosting: Boolean,
    progress: Float,
    animationsEnabled: Boolean,
    modeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xaraColors = LocalXaraColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "boost_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (animationsEnabled) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationsEnabled) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isBoosting) 1400 else 8000, easing = LinearEasing)
        ),
        label = "orbit_rot"
    )

    Box(
        modifier = modifier
            .size(164.dp)
            .testTag("boost_reactor_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        xaraColors.accentPrimary.copy(alpha = if (isBoosting) 0.35f else 0.22f),
                        xaraColors.accentSecondary.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                radius = radius * pulseScale
            )

            val ringInset = 10.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        xaraColors.accentPrimary,
                        xaraColors.accentSecondary,
                        Color.Transparent,
                        xaraColors.accentPrimary
                    )
                ),
                startAngle = rotationAngle,
                sweepAngle = if (isBoosting) 360f * progress.coerceAtLeast(0.15f) else 280f,
                useCenter = false,
                topLeft = Offset(ringInset, ringInset),
                size = Size(size.width - ringInset * 2, size.height - ringInset * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(124.dp)
                .scale(if (isBoosting) 0.96f else 1f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            xaraColors.accentPrimary,
                            xaraColors.accentSecondary
                        )
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                .minimumInteractiveComponentSize()
                .clickable(enabled = !isBoosting, onClick = onClick)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Boost Device",
                    tint = XaraObsidian,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = if (isBoosting) "${(progress * 100).toInt()}%" else "BOOST",
                    style = MaterialTheme.typography.headlineMedium,
                    color = XaraObsidian,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = XaraObsidian.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TelemetryMiniTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    XaraGlassCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        contentPadding = 12.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = XaraTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = XaraTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppIconImage(
    bitmap: Bitmap?,
    appName: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val xaraColors = LocalXaraColors.current
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(xaraColors.glassElevated)
            .border(1.dp, xaraColors.glassBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "$appName icon",
                modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(10.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Filled.SportsEsports,
                contentDescription = "$appName icon",
                tint = xaraColors.accentPrimary,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}

@Composable
fun StatusPillBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
