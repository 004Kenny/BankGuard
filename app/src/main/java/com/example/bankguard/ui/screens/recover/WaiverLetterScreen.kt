package com.example.bankguard.ui.screens.recover

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.letters.WaiverLetterTemplate
import com.example.bankguard.model.BankAccount
import com.example.bankguard.model.BankFee
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.WaiverReason
import com.example.bankguard.ui.components.ConfirmDialog
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.labelRes
import com.example.bankguard.ui.util.longDate
import kotlinx.coroutines.launch

/**
 * Build, edit, save, copy or share a fee-waiver letter. The user sends it themselves;
 * Share only opens Android's share sheet so they can pick their own email or messaging app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaiverLetterScreen(
    fee: BankFee,
    account: BankAccount,
    recovery: FeeRecovery?,
    onSave: (letter: String, reasons: Set<WaiverReason>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun build(reasons: Set<WaiverReason>) = WaiverLetterTemplate.build(
        fee = fee,
        account = account,
        reasons = reasons,
        todayText = longDate(0),
        feeDateText = longDate(fee.dayOffset),
    )

    // Reasons are saved as names so they survive rotation.
    var reasonNames by rememberSaveable { mutableStateOf(recovery?.reasons.orEmpty().map { it.name }) }
    val reasons = reasonNames.map(WaiverReason::valueOf).toSet()
    var lastBuilt by rememberSaveable { mutableStateOf(build(reasons)) }
    var text by rememberSaveable { mutableStateOf(recovery?.letter ?: lastBuilt) }
    var savedText by rememberSaveable { mutableStateOf(recovery?.letter) }
    var confirmRebuild by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    val unsaved = text != savedText
    val editedByUser = text != lastBuilt

    fun save() {
        onSave(text, reasons)
        savedText = text
    }

    fun rebuild() {
        lastBuilt = build(reasons)
        text = lastBuilt
    }

    fun leave() = if (unsaved) confirmLeave = true else onBack()

    // With the keyboard open, Back should only close the keyboard. On recent Android versions
    // (seen on Android 15) the app can receive that same Back press, so handle it here.
    val imeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    BackHandler {
        if (imeVisible) focusManager.clearFocus() else leave()
    }

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.le_title), onBack = ::leave)
        InfoNote(text = stringResource(R.string.le_template_note), tone = StatusTone.Info)

        SectionHeader(title = stringResource(R.string.le_reasons_title))
        SurfaceCard(verticalSpacing = Spacing.xs) {
            Text(stringResource(R.string.le_reasons_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            WaiverReason.entries.forEach { reason ->
                val checked = reason in reasons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouch)
                        .toggleable(
                            value = checked,
                            role = Role.Checkbox,
                            onValueChange = { on ->
                                val next = if (on) reasons + reason else reasons - reason
                                reasonNames = next.map { it.name }
                                // Untouched drafts follow the choices; edited ones wait for "Rebuild".
                                if (!editedByUser) {
                                    lastBuilt = build(next)
                                    text = lastBuilt
                                }
                            },
                        ),
                ) {
                    Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary))
                    Text(stringResource(reason.labelRes()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }
            if (editedByUser) {
                TonalButton(stringResource(R.string.le_rebuild), onClick = { confirmRebuild = true })
            }
        }

        SectionHeader(title = stringResource(R.string.le_letter_title))
        InfoNote(text = stringResource(R.string.le_placeholder_note), tone = StatusTone.Warning, iconRes = R.drawable.ic_info)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.le_letter_label)) },
            textStyle = MaterialTheme.typography.bodyMedium,
            minLines = 12,
            shape = BankGuardShapes.button,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.secondary, focusedLabelColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = stringResource(R.string.le_save),
            onClick = {
                save()
                Toast.makeText(context, R.string.le_saved, Toast.LENGTH_SHORT).show()
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val clipLabel = stringResource(R.string.le_title)
            SecondaryButton(
                text = stringResource(R.string.le_copy),
                leadingIcon = R.drawable.ic_description,
                modifier = Modifier.weight(1f),
                onClick = {
                    save()
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, text)))
                    }
                    Toast.makeText(context, R.string.le_copied, Toast.LENGTH_SHORT).show()
                },
            )
            val shareTitle = stringResource(R.string.le_share_title)
            val shareSubject = stringResource(R.string.le_share_subject)
            SecondaryButton(
                text = stringResource(R.string.le_share),
                leadingIcon = R.drawable.ic_arrow_forward,
                modifier = Modifier.weight(1f),
                onClick = {
                    save()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                },
            )
        }
        InfoNote(text = stringResource(R.string.le_send_note))
    }

    if (confirmRebuild) {
        ConfirmDialog(
            title = stringResource(R.string.le_rebuild_title),
            body = stringResource(R.string.le_rebuild_body),
            confirmLabel = stringResource(R.string.le_rebuild_yes),
            dismissLabel = stringResource(R.string.le_rebuild_no),
            onConfirm = {
                confirmRebuild = false
                rebuild()
            },
            onDismiss = { confirmRebuild = false },
        )
    }
    if (confirmLeave) {
        ConfirmDialog(
            title = stringResource(R.string.le_leave_title),
            body = stringResource(R.string.le_leave_body),
            confirmLabel = stringResource(R.string.le_leave_save),
            dismissLabel = stringResource(R.string.le_leave_discard),
            onConfirm = {
                confirmLeave = false
                save()
                onBack()
            },
            onDismiss = {
                confirmLeave = false
                onBack()
            },
            // Tapping outside just closes the question; it doesn't discard anything.
            onDismissRequest = { confirmLeave = false },
        )
    }
}

@Preview(showBackground = true, heightDp = 1900)
@Composable
private fun WaiverLetterPreview() {
    val alex = DemoProfiles.underbanked
    BankGuardTheme {
        WaiverLetterScreen(alex.bankFees.first(), alex.account!!, null, { _, _ -> }, {})
    }
}
