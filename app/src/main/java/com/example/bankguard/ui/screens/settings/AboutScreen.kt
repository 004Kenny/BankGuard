package com.example.bankguard.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.bankguard.R
import com.example.bankguard.ui.components.BankGuardLogo
import com.example.bankguard.ui.components.DisclosureCard
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

private data class Disclosure(@param:DrawableRes val iconRes: Int, @param:StringRes val titleRes: Int, @param:StringRes val bodyRes: Int)

private val disclosures = listOf(
    Disclosure(R.drawable.ic_science, R.string.about_demo_title, R.string.about_demo_body),
    Disclosure(R.drawable.ic_schedule, R.string.about_partner_title, R.string.about_partner_body),
    Disclosure(R.drawable.ic_published_with_changes, R.string.about_refunds_title, R.string.about_refunds_body),
    Disclosure(R.drawable.ic_description, R.string.about_letters_title, R.string.about_letters_body),
    Disclosure(R.drawable.ic_insights, R.string.about_estimates_title, R.string.about_estimates_body),
    Disclosure(R.drawable.ic_lock, R.string.about_privacy_title, R.string.about_privacy_body),
)

private data class License(@param:StringRes val labelRes: Int, val assetPath: String)

private val licenses = listOf(
    License(R.string.about_license_jakarta, "licenses/OFL-PlusJakartaSans.txt"),
    License(R.string.about_license_inter, "licenses/OFL-Inter.txt"),
    License(R.string.about_license_symbols, "licenses/Apache-2.0-material-symbols.txt"),
)

/** What BankGuard is and isn't, every demo disclosure in one place, and open-source licenses. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.about_title), onBack = onBack)

        HeroCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                BankGuardLogo(container = BankGuardTheme.colors.heroInset)
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    HeroLabel(stringResource(R.string.about_hero_label))
                }
            }
            Text(stringResource(R.string.about_hero_body), style = MaterialTheme.typography.bodyMedium, color = BankGuardTheme.colors.onHeroMuted)
        }

        SectionHeader(title = stringResource(R.string.about_disclosures))
        SurfaceCard(verticalSpacing = Spacing.md) {
            disclosures.forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    IconBox(iconRes = d.iconRes, tone = StatusTone.Brand)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text(stringResource(d.titleRes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
                        Text(stringResource(d.bodyRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        DisclosureCard(title = stringResource(R.string.disc_title), body = stringResource(R.string.disc_body))

        SectionHeader(title = stringResource(R.string.about_licenses))
        SurfaceCard(verticalSpacing = Spacing.xs) {
            licenses.forEachIndexed { i, license ->
                if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                LicenseRow(license)
            }
            HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
            Text(stringResource(R.string.about_license_jetpack), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LicenseRow(license: License) {
    val context = LocalContext.current
    var expanded by rememberSaveable(license.assetPath) { mutableStateOf(false) }
    // Read only when opened; the files are small text assets bundled with the app.
    val text = remember(expanded) {
        if (!expanded) null else runCatching { context.assets.open(license.assetPath).bufferedReader().use { it.readText() } }.getOrNull()
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = Sizes.minTouch)) {
            Text(stringResource(license.labelRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    stringResource(if (expanded) R.string.about_license_hide else R.string.about_license_show),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        if (text != null) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun AboutPreview() {
    BankGuardTheme { AboutScreen(onBack = {}) }
}
