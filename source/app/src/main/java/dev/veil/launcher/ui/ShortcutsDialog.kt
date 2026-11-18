@file:OptIn(ExperimentalTvMaterial3Api::class)

package dev.veil.launcher.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.veil.launcher.LauncherState
import dev.veil.launcher.Shortcuts
import kotlinx.coroutines.delay

/** Whether the Shortcuts window is open. */
object ShortcutsUi {
    var open by mutableStateOf(false)
}

private const val START = -1

/** Choose the app that opens when the TV starts, and the app each remote button opens. */
@Composable
fun ShortcutsDialog(state: LauncherState) {
    val context = LocalContext.current
    // null = overview, START = choosing the start app, otherwise the key code being mapped
    var pick by remember { mutableStateOf<Int?>(null) }
    var version by remember { mutableStateOf(0) }
    val first = remember { FocusRequester() }
    val close = { ShortcutsUi.open = false; state.menuReturn = "shortcuts" }
    val reload = version // read so the lists below refresh after saving

    LaunchedEffect(pick) {
        delay(120)
        runCatching { first.requestFocus() }
    }

    fun labelOf(pkg: String?): String =
        if (pkg == null) "Off" else (state.apps.firstOrNull { it.pkg == pkg }?.label ?: "Not installed")

    val save: (String?) -> Unit = { pkg ->
        val p = pick
        if (p == START) Shortcuts.setStartApp(context, pkg)
        else if (p != null) Shortcuts.setKeyApp(context, p, pkg)
        version = version + 1
        pick = null
    }

    Dialog(onDismissRequest = { if (pick != null) { pick = null } else { close() } }) {
        Column(
            Modifier
                .width(420.dp)
                .heightIn(max = 460.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Veil.Sheet)
                .border(1.dp, Veil.Line, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val p = pick
            val title = when {
                p == null -> "Shortcuts"
                p == START -> "Start app"
                else -> Shortcuts.KEYS.firstOrNull { it.first == p }?.second ?: "Button"
            }
            val hint = when {
                p == null -> "Open an app when the TV starts, or with a remote button on the Home screen. Back to close."
                p == START -> "This app opens when the TV starts. Back to go back."
                else -> "This app opens when you press the button on the Home screen. Back to go back."
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(hint, fontSize = 10.sp, color = Veil.Text.copy(alpha = 0.6f))

            Column(
                Modifier.verticalScroll(remember(pick) { ScrollState(0) }),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (p == null) {
                    ShortcutRow(
                        title = "Start app",
                        value = labelOf(Shortcuts.startApp(context)),
                        modifier = Modifier.focusRequester(first),
                    ) { pick = START }
                    Shortcuts.KEYS.forEach { (code, name) ->
                        ShortcutRow(title = name, value = labelOf(Shortcuts.keyApp(context, code))) { pick = code }
                    }
                } else {
                    val current = if (p == START) Shortcuts.startApp(context) else Shortcuts.keyApp(context, p)
                    ShortcutRow(
                        title = "Off",
                        value = "",
                        selected = current == null,
                        modifier = Modifier.focusRequester(first),
                    ) { save(null) }
                    state.apps.forEach { app ->
                        ShortcutRow(title = app.label, value = "", selected = current == app.pkg) { save(app.pkg) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.06f), contentColor = Veil.Text,
            focusedContainerColor = Veil.Text, focusedContentColor = Veil.Ink,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (value.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    value, fontSize = 10.sp, color = LocalContentColor.current.copy(alpha = 0.65f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(VeilIcons.Check, contentDescription = "Selected", modifier = Modifier.size(12.dp), tint = LocalContentColor.current)
            }
        }
    }
}
