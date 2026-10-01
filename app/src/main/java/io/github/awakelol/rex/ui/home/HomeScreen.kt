package io.github.awakelol.rex.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.awakelol.rex.R
import io.github.awakelol.rex.ui.theme.SafeGreen
import io.github.awakelol.rex.ui.theme.WarnAmber
import io.github.awakelol.rex.warning.callLabel

@Composable
fun HomeScreen(
    needsFixing: Boolean,
    helperName: String,
    helperPhone: String,
    onCall: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val background = if (needsFixing) WarnAmber else SafeGreen
    val foreground = if (needsFixing) Color.Black else Color.White

    Column(
        Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Text(
                    stringResource(if (needsFixing) R.string.home_needs_fixing else R.string.home_protected),
                    color = foreground,
                    fontSize = 42.sp,
                    lineHeight = 50.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (needsFixing) {
                    if (helperPhone.isNotBlank()) {
                        OutlinedButton(
                            onClick = onCall,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                            border = BorderStroke(3.dp, Color.Black),
                        ) {
                            Text(callLabel(helperName), color = Color.Black, fontSize = 32.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        Text(
                            stringResource(R.string.home_ask_for_help),
                            color = foreground,
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        TextButton(onClick = onOpenAdmin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                if (helperName.isBlank()) {
                    stringResource(R.string.home_admin_button)
                } else {
                    stringResource(R.string.home_admin_button_named, helperName)
                },
                color = foreground.copy(alpha = 0.8f),
                fontSize = 16.sp,
            )
        }
    }
}
