package com.example.bankguard.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.ui.theme.BankGuardShapes

/** Small pill that states a status, such as "High priority" or "Approved". */
@Composable
fun StatusBadge(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = tone.colors()
    Surface(
        modifier = modifier,
        shape = BankGuardShapes.pill,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Marks a feature as planned but not built yet. */
@Composable
fun ComingSoonBadge(modifier: Modifier = Modifier) {
    StatusBadge(text = stringResource(R.string.coming_soon), tone = StatusTone.Info, modifier = modifier)
}
