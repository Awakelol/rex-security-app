package io.github.awakelol.rex.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.awakelol.rex.R
import io.github.awakelol.rex.container
import io.github.awakelol.rex.core.PinHasher
import io.github.awakelol.rex.data.PinCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinScreen(hasPin: Boolean, onUnlocked: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        if (hasPin) EnterPin(onUnlocked) else CreatePin(onUnlocked)
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun EnterPin(onUnlocked: () -> Unit) {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var checking by remember { mutableStateOf(false) }

    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val locked = now < lockedUntil

    val res = LocalResources.current
    fun submit() {
        if (checking || locked) return
        checking = true
        scope.launch {
            when (val r = settings.checkPin(pin)) {
                PinCheck.Ok -> onUnlocked()
                is PinCheck.Wrong -> error = res.getQuantityString(R.plurals.pin_wrong, r.triesLeft, r.triesLeft)
                is PinCheck.LockedOut -> {
                    error = null
                    lockedUntil = r.until
                }
            }
            pin = ""
            checking = false
        }
    }

    Text(stringResource(R.string.pin_enter_title), style = MaterialTheme.typography.headlineMedium)
    PinField(pin, enabled = !locked) { pin = it }
    if (locked) {
        val seconds = ((lockedUntil - now) / 1000 + 1).toInt()
        Text(pluralStringResource(R.plurals.pin_locked, seconds, seconds), color = MaterialTheme.colorScheme.error)
    } else if (error != null) {
        Text(error!!, color = MaterialTheme.colorScheme.error)
    }
    Button(onClick = ::submit, enabled = !locked && !checking && pin.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.pin_unlock))
    }
}

@Composable
private fun CreatePin(onCreated: () -> Unit) {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    Text(stringResource(R.string.pin_create_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.pin_create_hint))
    PinField(first, label = stringResource(R.string.pin_new)) { first = it }
    PinField(second, label = stringResource(R.string.pin_confirm)) { second = it }
    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = {
            error = when {
                !PinHasher.isValidPin(first) -> R.string.pin_invalid
                first != second -> R.string.pin_mismatch
                else -> null
            }
            if (error == null) scope.launch {
                settings.setPin(first)
                onCreated()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.pin_save))
    }
}

@Composable
fun PinField(value: String, label: String? = null, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(8)) },
        label = label?.let { { Text(it) } },
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}
