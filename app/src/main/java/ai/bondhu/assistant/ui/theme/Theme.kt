package ai.bondhu.assistant.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val Dark = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF66D9EF),
    secondary = androidx.compose.ui.graphics.Color(0xFF7BFFB2),
    surface = androidx.compose.ui.graphics.Color(0xFF0B1F3A),
)
private val Light = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0B1F3A),
    secondary = androidx.compose.ui.graphics.Color(0xFF00796B),
)

@Composable
fun BondhuTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) Dark else Light
    MaterialTheme(colorScheme = scheme, content = content)
}
