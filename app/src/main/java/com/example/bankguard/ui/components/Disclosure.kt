package com.example.bankguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.bankguard.R
import com.example.bankguard.ui.theme.Spacing

/** States plainly how BankGuard could benefit from a recommendation. Always shown next to account suggestions. */
@Composable
fun DisclosureCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBox(iconRes = R.drawable.ic_verified_user, tone = StatusTone.Brand)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
