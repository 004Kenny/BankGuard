package com.example.bankguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankguard.domain.forecast.ForecastDay
import com.example.bankguard.ui.theme.BankGuardTheme

/**
 * Line chart of projected daily balances with a marker on the lowest day
 * (from the Stitch Overview balance card). Draws a dashed zero line when the balance goes negative.
 * Screen readers get [summary] instead of the drawing.
 */
@Composable
fun ForecastChart(
    days: List<ForecastDay>,
    summary: String,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    height: Dp = 88.dp,
    lineColor: Color = BankGuardTheme.colors.heroAccent,
    negativeColor: Color = BankGuardTheme.colors.onHeroCritical,
    guideColor: Color = BankGuardTheme.colors.onHeroMuted,
    labelColor: Color = BankGuardTheme.colors.onHeroMuted,
) {
    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = summary }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clearAndSetSemantics { },
        ) {
            if (days.size < 2) return@Canvas
            val values = days.map { it.balance.cents.toFloat() }
            val hasNegative = values.any { it < 0f }
            val minV = values.min()
            val maxV = values.max()
            val lo = if (hasNegative) minOf(minV, 0f) else minV
            val span = (maxV - lo).takeIf { it > 0f } ?: 1f
            val vPad = 10.dp.toPx()
            val hPad = 6.dp.toPx()
            fun x(i: Int) = hPad + (size.width - 2 * hPad) * i / (days.size - 1)
            fun y(v: Float) = vPad + (size.height - 2 * vPad) * (1f - (v - lo) / span)

            val line = Path().apply {
                moveTo(x(0), y(values[0]))
                for (i in 1 until values.size) lineTo(x(i), y(values[i]))
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(x(values.lastIndex), size.height)
                lineTo(x(0), size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)))

            if (hasNegative) {
                val zeroY = y(0f)
                drawLine(
                    color = guideColor.copy(alpha = 0.7f),
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            }
            drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            val lowIndex = values.indices.minBy { values[it] }
            val markerColor = if (values[lowIndex] < 0f) negativeColor else lineColor
            val center = Offset(x(lowIndex), y(values[lowIndex]))
            drawCircle(color = markerColor.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = center)
            drawCircle(color = markerColor, radius = 5.dp.toPx(), center = center)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = labelColor, modifier = Modifier.weight(1f))
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}
