package io.github.awakelol.rex.warning

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.awakelol.rex.R
import io.github.awakelol.rex.container
import io.github.awakelol.rex.data.PendingApp
import io.github.awakelol.rex.ui.dial
import io.github.awakelol.rex.ui.theme.DangerRed
import io.github.awakelol.rex.ui.theme.RexTheme
import io.github.awakelol.rex.ui.theme.SafeGreen
import io.github.awakelol.rex.ui.uninstall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WarningActivity : ComponentActivity() {

    private var pkg by mutableStateOf<String?>(null)
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        pkg = intent.getStringExtra(EXTRA_PACKAGE)

        setContent {
            RexTheme {
                val current = pkg
                if (current == null) {
                    LaunchedEffect(Unit) { finish() }
                } else {
                    WarningContent(current)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pkg = intent.getStringExtra(EXTRA_PACKAGE)
    }

    override fun onResume() {
        super.onResume()
        resumes++  // re-check after coming back from the uninstall dialog
    }

    @Composable
    private fun WarningContent(pkg: String) {
        val c = container
        val state by produceState<WarningState>(WarningState.Loading, pkg, resumes) {
            value = withContext(Dispatchers.IO) {
                val app = c.db.pending().get(pkg)
                when {
                    !c.inspector.isInstalled(pkg) -> WarningState.Removed
                    app == null -> WarningState.NotPending
                    else -> WarningState.Show(app, c.inspector.icon(pkg)?.toBitmap(256, 256)?.asImageBitmap())
                }
            }
        }
        val helperName by c.settings.helperName.collectAsState(initial = "")
        val helperPhone by c.settings.helperPhone.collectAsState(initial = "")

        Surface(Modifier.fillMaxSize(), color = Color.White) {
            when (val s = state) {
                WarningState.Loading -> Unit
                WarningState.NotPending -> LaunchedEffect(Unit) { finish() }
                WarningState.Removed -> RemovedScreen(onDone = ::finish)
                is WarningState.Show -> DangerScreen(
                    app = s.app,
                    icon = s.icon,
                    helperName = helperName,
                    helperPhone = helperPhone,
                    onRemove = { uninstall(pkg) },
                    onCall = { dial(helperPhone) },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, pkg: String): Intent =
            Intent(context, WarningActivity::class.java)
                .putExtra(EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private sealed interface WarningState {
    data object Loading : WarningState
    data object NotPending : WarningState
    data object Removed : WarningState
    data class Show(val app: PendingApp, val icon: ImageBitmap?) : WarningState
}

@Composable
private fun DangerScreen(
    app: PendingApp,
    icon: ImageBitmap?,
    helperName: String,
    helperPhone: String,
    onRemove: () -> Unit,
    onCall: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            stringResource(R.string.warning_title),
            color = DangerRed,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(112.dp))
        }
        Text(
            app.label,
            color = Color.Black,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp,
        )
        Text(
            stringResource(R.string.warning_body),
            color = Color.Black,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
        ) {
            Text(stringResource(R.string.warning_remove), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        if (helperPhone.isNotBlank()) {
            OutlinedButton(
                onClick = onCall,
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                border = BorderStroke(3.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            ) {
                Text(callLabel(helperName), fontSize = 28.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun RemovedScreen(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.removed_title),
            color = SafeGreen,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.removed_body),
            color = Color.Black,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp,
        )
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
        ) {
            Text(stringResource(R.string.ok), fontSize = 32.sp)
        }
    }
}

@Composable
fun callLabel(helperName: String): String =
    if (helperName.isBlank()) stringResource(R.string.call_helper) else stringResource(R.string.call_person, helperName)
