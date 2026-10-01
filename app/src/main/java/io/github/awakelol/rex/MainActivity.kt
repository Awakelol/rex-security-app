package io.github.awakelol.rex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.awakelol.rex.ui.admin.AdminScreen
import io.github.awakelol.rex.ui.admin.PinScreen
import io.github.awakelol.rex.ui.dial
import io.github.awakelol.rex.ui.home.HomeScreen
import io.github.awakelol.rex.ui.rememberProtectionStatus
import io.github.awakelol.rex.ui.theme.RexTheme
import io.github.awakelol.rex.watch.GuardService
import kotlinx.coroutines.launch

private enum class Screen { HOME, PIN, ADMIN }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container.scope.launch { container.watcher.ensureBaseline() }

        setContent {
            RexTheme {
                Surface(Modifier.fillMaxSize()) {
                    Content()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        GuardService.start(this)
    }

    override fun onStop() {
        super.onStop()
        // Lock the helper area whenever the app leaves the screen (but not on rotation).
        if (!isChangingConfigurations) container.adminUnlocked.value = false
    }

    @Composable
    private fun Content() {
        val c = container
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        val unlocked by c.adminUnlocked.collectAsState()
        val hasPin by c.settings.hasPin.collectAsState(initial = null)
        val helperName by c.settings.helperName.collectAsState(initial = "")
        val helperPhone by c.settings.helperPhone.collectAsState(initial = "")
        val status = rememberProtectionStatus()

        LaunchedEffect(unlocked) {
            if (!unlocked && screen == Screen.ADMIN) screen = Screen.HOME
        }
        BackHandler(enabled = screen != Screen.HOME) {
            c.adminUnlocked.value = false
            screen = Screen.HOME
        }

        when (screen) {
            Screen.HOME -> HomeScreen(
                needsFixing = status.needsFixing,
                helperName = helperName,
                helperPhone = helperPhone,
                onCall = { dial(helperPhone) },
                onOpenAdmin = { screen = Screen.PIN },
            )
            Screen.PIN -> hasPin?.let { pinSet ->
                PinScreen(
                    hasPin = pinSet,
                    onUnlocked = {
                        c.adminUnlocked.value = true
                        screen = Screen.ADMIN
                    },
                    onCancel = { screen = Screen.HOME },
                )
            }
            Screen.ADMIN -> if (unlocked) {
                AdminScreen(onLock = {
                    c.adminUnlocked.value = false
                    screen = Screen.HOME
                })
            }
        }
    }
}
