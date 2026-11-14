package com.example.bankguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Spacing

/** App bar shared by all tabs: logo, wordmark, demo tag and an optional profile button. */
@Composable
fun BankGuardTopBar(
    modifier: Modifier = Modifier,
    onProfileClick: (() -> Unit)? = null,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = Spacing.screen),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                BankGuardLogo()
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                DemoModeTag()
                Spacer(Modifier.weight(1f))
                if (onProfileClick != null) {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person),
                            contentDescription = stringResource(R.string.cd_demo_profile),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Spacer(Modifier.width(Spacing.xs))
                }
            }
            HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        }
    }
}

/** Navy rounded square with the aqua shield mark (from the Stitch onboarding logo). */
@Composable
fun BankGuardLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    /** Override when the logo sits on a navy surface, so its square stays visible. */
    container: Color = BankGuardTheme.colors.heroStart,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(container, BankGuardShapes.iconBox),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = BankGuardTheme.colors.heroAccent,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}
