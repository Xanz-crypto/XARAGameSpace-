package com.xara.gamespace.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary

@Composable
fun XaraCosmicBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val xaraColors = LocalXaraColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XaraObsidian)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        xaraColors.accentSecondary.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.maxDimension * 0.55f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        xaraColors.accentPrimary.copy(alpha = 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.45f),
                    radius = size.maxDimension * 0.5f
                )
            )
            val gridStep = 44.dp.toPx()
            val gridColor = xaraColors.glassBorder.copy(alpha = 0.12f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += gridStep
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += gridStep
            }
        }
        content()
    }
}

@Composable
fun XaraGlassCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val xaraColors = LocalXaraColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = if (accentBorder) {
        Brush.linearGradient(
            listOf(
                xaraColors.accentPrimary.copy(alpha = 0.8f),
                xaraColors.accentSecondary.copy(alpha = 0.65f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                xaraColors.glassBorder.copy(alpha = 0.85f),
                xaraColors.glassBorder.copy(alpha = 0.35f)
            )
        )
    }

    val surfaceModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    xaraColors.glassElevated.copy(alpha = 0.82f),
                    xaraColors.glassSurface.copy(alpha = 0.90f)
                )
            )
        )
        .border(BorderStroke(1.dp, borderBrush), shape)
        .then(
            if (onClick != null) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(contentPadding)

    Column(modifier = surfaceModifier, content = content)
}

@Composable
fun XaraLogoCrest(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val xaraColors = LocalXaraColors.current
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pad = w * 0.12f

        // Outer hexagonal frame
        val hexPath = Path().apply {
            moveTo(w * 0.5f, pad * 0.4f)
            lineTo(w - pad * 0.6f, h * 0.28f)
            lineTo(w - pad * 0.6f, h * 0.72f)
            lineTo(w * 0.5f, h - pad * 0.4f)
            lineTo(pad * 0.6f, h * 0.72f)
            lineTo(pad * 0.6f, h * 0.28f)
            close()
        }
        drawPath(
            path = hexPath,
            brush = Brush.linearGradient(
                listOf(xaraColors.accentPrimary, xaraColors.accentSecondary)
            ),
            style = Stroke(width = w * 0.055f)
        )

        // Geometric X blades
        drawLine(
            brush = Brush.linearGradient(
                listOf(xaraColors.accentPrimary, xaraColors.accentSecondary)
            ),
            start = Offset(w * 0.26f, h * 0.26f),
            end = Offset(w * 0.74f, h * 0.74f),
            strokeWidth = w * 0.11f,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.linearGradient(
                listOf(xaraColors.accentSecondary, xaraColors.accentPrimary)
            ),
            start = Offset(w * 0.74f, h * 0.26f),
            end = Offset(w * 0.26f, h * 0.74f),
            strokeWidth = w * 0.11f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = xaraColors.accentPrimary,
            radius = w * 0.06f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

@Composable
fun XaraSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val xaraColors = LocalXaraColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(xaraColors.accentPrimary, xaraColors.accentSecondary)
                        )
                    )
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = XaraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = XaraTextSecondary
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun XaraTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null
) {
    val xaraColors = LocalXaraColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = xaraColors.glassElevated,
            shape = CircleShape,
            border = BorderStroke(1.dp, xaraColors.glassBorder),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("top_bar_back_button")
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = xaraColors.accentPrimary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = XaraTextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = xaraColors.accentPrimary
                )
            }
        }
        actions?.invoke()
    }
}
