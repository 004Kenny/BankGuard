package com.example.bankguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

/** Dashed rounded border, the Stitch signal for demo or simulated content. */
fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, strokeWidth: Dp = 1.dp): Modifier =
    drawBehind {
        val stroke = strokeWidth.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }

/** Compact "DEMO MODE" pill shown in the top bar and on demo figures. */
@Composable
fun DemoModeTag(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.demo_mode),
) {
    val colors = BankGuardTheme.colors
    Row(
        modifier = modifier
            .background(colors.demoContainer, BankGuardShapes.pill)
            .dashedBorder(colors.demoBorder, cornerRadius = 50.dp)
            .padding(horizontal = 10.dp, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_science),
            contentDescription = null,
            tint = colors.demo,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.demo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Full-width notice that the figures on a screen are fictional. */
@Composable
fun SimulatedDataBanner(
    text: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.simulated_data_title),
) {
    val colors = BankGuardTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.demoContainer, BankGuardShapes.inset)
            .dashedBorder(colors.demoBorder.copy(alpha = 0.6f), cornerRadius = 12.dp)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_science),
            contentDescription = null,
            tint = colors.demo,
            modifier = Modifier
                .padding(top = Spacing.xxs)
                .size(Sizes.iconSmall),
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = colors.demo)
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.demo)
        }
    }
}
