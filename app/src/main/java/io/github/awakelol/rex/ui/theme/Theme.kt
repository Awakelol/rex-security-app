package io.github.awakelol.rex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SafeGreen = Color(0xFF1B5E20)
val WarnAmber = Color(0xFFFFB300)
val DangerRed = Color(0xFFB71C1C)

// Fixed light scheme on purpose: no dynamic color, so contrast stays predictable
// whatever wallpaper or theme the phone has.
private val colors = lightColorScheme(
    primary = SafeGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF263238),
    onSecondary = Color.White,
    error = DangerRed,
    onError = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
)

@Composable
fun RexTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
