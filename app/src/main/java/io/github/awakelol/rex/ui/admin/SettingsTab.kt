package io.github.awakelol.rex.ui.admin

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.awakelol.rex.R
import io.github.awakelol.rex.alert.Discord
import io.github.awakelol.rex.container
import io.github.awakelol.rex.core.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsTab() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HelperSection()
        HorizontalDivider()
        WebhookSection()
        HorizontalDivider()
        ChangePinSection()
        HorizontalDivider()
        RebaselineSection()
    }
}

@Composable
private fun HelperSection() {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        name = settings.helperName.first()
        phone = settings.helperPhone.first()
    }

    Section(stringResource(R.string.settings_helper)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; saved = false },
            label = { Text(stringResource(R.string.settings_helper_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; saved = false },
            label = { Text(stringResource(R.string.settings_helper_phone)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { scope.launch { settings.setHelper(name, phone); saved = true } }) {
            Text(stringResource(if (saved) R.string.saved else R.string.save))
        }
    }
}

@Composable
private fun WebhookSection() {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { url = settings.webhookUrl.first() }

    val savedMsg = stringResource(R.string.saved)
    val needsHttps = stringResource(R.string.webhook_https)
    val sentMsg = stringResource(R.string.webhook_sent)
    val failedMsg = stringResource(R.string.webhook_failed)
    val testText = stringResource(R.string.webhook_test_text, Build.MANUFACTURER, Build.MODEL)

    Section(stringResource(R.string.settings_webhook)) {
        Text(stringResource(R.string.settings_webhook_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim(); message = null },
            label = { Text(stringResource(R.string.settings_webhook_url)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                if (url.isNotEmpty() && !url.startsWith("https://")) {
                    message = needsHttps
                } else {
                    scope.launch { settings.setWebhookUrl(url); message = savedMsg }
                }
            }) { Text(stringResource(R.string.save)) }

            OutlinedButton(
                enabled = url.startsWith("https://") && !sending,
                onClick = {
                    sending = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { Discord.post(url, testText) }
                        message = when (result) {
                            Discord.Result.Sent -> sentMsg
                            is Discord.Result.TryLater -> failedMsg.format(result.why)
                            is Discord.Result.Failed -> failedMsg.format(result.why)
                        }
                        sending = false
                    }
                },
            ) { Text(stringResource(R.string.webhook_test)) }
        }
        message?.let { Text(it) }
    }
}

@Composable
private fun ChangePinSection() {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Int?>(null) }

    Section(stringResource(R.string.settings_pin)) {
        PinField(first, label = stringResource(R.string.pin_new)) { first = it; result = null }
        PinField(second, label = stringResource(R.string.pin_confirm)) { second = it; result = null }
        Button(onClick = {
            when {
                !PinHasher.isValidPin(first) -> result = R.string.pin_invalid
                first != second -> result = R.string.pin_mismatch
                else -> scope.launch {
                    settings.setPin(first)
                    first = ""
                    second = ""
                    result = R.string.pin_changed
                }
            }
        }) { Text(stringResource(R.string.pin_save)) }
        result?.let { Text(stringResource(it)) }
    }
}

@Composable
private fun RebaselineSection() {
    val watcher = LocalContext.current.container.watcher
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }

    Section(stringResource(R.string.settings_rebaseline)) {
        Text(stringResource(R.string.settings_rebaseline_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { confirm = true }) { Text(stringResource(R.string.settings_rebaseline_button)) }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.settings_rebaseline)) },
            text = { Text(stringResource(R.string.rebaseline_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch { watcher.rebaseline() }
                }) { Text(stringResource(R.string.approve_all)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
