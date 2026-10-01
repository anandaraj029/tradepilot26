package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecisionType
import com.example.data.model.RiskStatus
import com.example.ui.theme.*

@Composable
fun FintechCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceDark,
    borderColor: Color = BorderDark,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun DecisionBadge(
    decision: DecisionType,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (decision) {
        DecisionType.BUY -> Pair(BullishGreen.copy(alpha = 0.15f), BullishGreen)
        DecisionType.SELL, DecisionType.EXIT -> Pair(BearishRed.copy(alpha = 0.15f), BearishRed)
        DecisionType.HOLD -> Pair(CyanBlue.copy(alpha = 0.15f), CyanBlue)
        DecisionType.WAIT -> Pair(AmberWarning.copy(alpha = 0.15f), AmberWarning)
        DecisionType.REDUCE_POSITION -> Pair(BearishRed.copy(alpha = 0.15f), BearishRed)
        DecisionType.NO_TRADE -> Pair(BearishRed.copy(alpha = 0.25f), BearishRed)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = bg,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = decision.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RiskStatusBadge(
    status: RiskStatus,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (status) {
        RiskStatus.SAFE -> Pair(BullishGreen.copy(alpha = 0.15f), BullishGreen)
        RiskStatus.CAUTION -> Pair(AmberWarning.copy(alpha = 0.15f), AmberWarning)
        RiskStatus.CRITICAL -> Pair(BearishRed.copy(alpha = 0.2f), BearishRed)
        RiskStatus.LOCKED -> Pair(BearishRed, Color.White)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = bg,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (status == RiskStatus.LOCKED) Icons.Default.Dangerous else Icons.Default.Shield,
                contentDescription = status.label,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun Sparkline(
    data: List<Float>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val color = if (isPositive) BullishGreen else BearishRed

    Canvas(modifier = modifier) {
        val minVal = data.minOrNull() ?: 0f
        val maxVal = data.maxOrNull() ?: 1f
        val range = (maxVal - minVal).coerceAtLeast(0.001f)

        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1)

        val path = Path()
        val fillPath = Path()

        data.forEachIndexed { i, value ->
            val x = i * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = normalizedY * (h - 8.dp.toPx()) + 4.dp.toPx()

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == data.size - 1) {
                fillPath.lineTo(x, h)
                fillPath.close()
            }
        }

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun MetricProgressBar(
    label: String,
    value: Int,
    max: Int = 100,
    accentColor: Color = ElectricTeal,
    modifier: Modifier = Modifier
) {
    val progress = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$value / $max",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.7f), accentColor)
                        )
                    )
            )
        }
    }
}
