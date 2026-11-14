package com.example.bankguard.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.ui.components.BankGuardLogo
import com.example.bankguard.ui.components.DemoModeTag
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing
import kotlinx.coroutines.launch

private data class IntroPage(
    @param:DrawableRes val iconRes: Int?,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

private val pages = listOf(
    IntroPage(null, R.string.intro_welcome_title, R.string.intro_welcome_body),
    IntroPage(R.drawable.ic_shield, R.string.intro_prevent_title, R.string.intro_prevent_body),
    IntroPage(R.drawable.ic_published_with_changes, R.string.intro_recover_title, R.string.intro_recover_body),
    IntroPage(R.drawable.ic_savings, R.string.intro_discover_title, R.string.intro_discover_body),
)

/** Short swipeable introduction to the three pillars, shown before persona selection. */
@Composable
fun WelcomeScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DemoModeTag()
                Spacer(Modifier.weight(1f))
                if (!isLast) {
                    TextButton(onClick = onFinish) {
                        Text(stringResource(R.string.intro_skip), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                PageContent(pages[index])
            }

            PageDots(count = pages.size, current = pagerState.currentPage)
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(
                text = stringResource(if (isLast) R.string.intro_start else R.string.intro_next),
                trailingIcon = R.drawable.ic_arrow_forward,
                onClick = {
                    if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
        }
    }
}

@Composable
private fun PageContent(page: IntroPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (page.iconRes == null) {
            BankGuardLogo(size = 96.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, BankGuardShapes.card),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(page.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val description = stringResource(R.string.intro_page, current + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.iconSmall)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .background(color, BankGuardShapes.pill),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun WelcomePreview() {
    BankGuardTheme { WelcomeScreen(onFinish = {}) }
}
