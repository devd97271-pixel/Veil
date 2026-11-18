@file:OptIn(ExperimentalTvMaterial3Api::class)

package dev.veil.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.veil.launcher.LauncherState
import dev.veil.launcher.data.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The "veil" wordmark is a button. While it — or anything in its menu — has focus, the Veil menu drops
 * down under it: start Veil Ambient, pick a theme, change the weather location. Press Down to enter it.
 */
@Composable
fun VeilMenuAnchor(state: LauncherState) {
    var focused by remember { mutableStateOf(false) }
    // Open only after focus has rested here briefly: when Veil comes to the front, focus passes over the
    // wordmark for a few ms before landing on Continue Watching, which would otherwise flash the menu.
    LaunchedEffect(focused) {
        if (focused) { delay(200); if (!state.veilMenuOpen) Sfx.open(); state.veilMenuOpen = true } else state.veilMenuOpen = false
    }
    Box(Modifier.onFocusChanged { focused = it.hasFocus; state.wordmarkFocused = it.hasFocus }) {
        // Styled like the other top-bar pills (soft fill + outline + chevron) so it reads as a button.
        Surface(
            onClick = { /* the menu opens on focus; OK does nothing extra */ },
            // Back closes the menu from the "veil" button too (not only from inside the menu), by sending
            // focus back to the page, exactly like the panel's own Back handling.
            modifier = Modifier.focusSound(1.12f).onPreviewKeyEvent { e ->
                if (e.key == Key.Back && e.type == KeyEventType.KeyUp) { Sfx.back(); state.focusHome(); true }
                else e.key == Key.Back
            },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Veil.Glass, contentColor = Veil.Text,
                focusedContainerColor = Veil.Text, focusedContentColor = Veil.Ink,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Veil.Line), shape = RoundedCornerShape(50)),
                focusedBorder = Border.None,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        ) {
            Row(Modifier.padding(start = 9.dp, end = 9.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                // December: a snowflake instead of the lock (if seasonal touches are on).
                Icon(
                    if (state.seasonal && Seasons.isWinter()) VeilIcons.Snowflake else VeilIcons.Lock,
                    contentDescription = null, modifier = Modifier.size(13.dp), tint = LocalContentColor.current,
                )
                Spacer(Modifier.width(6.dp))
                Text("veil", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
                Spacer(Modifier.width(5.dp))
                Icon(
                    VeilIcons.ChevronDown, contentDescription = "Menu", modifier = Modifier.size(11.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.7f),
                )
            }
        }
        Box(Modifier.floatingBelow(40.dp)) {
            // Kept built while on Home (it was rebuilt from scratch on every open, which stuttered); while
            // closed it isn't placed on screen, so the remote can never move focus into it.
           KeptPanel(open = state.veilMenuOpen) { VeilPanel(state) }
           if (ShortcutsUi.open) ShortcutsDialog(state)
        }
    }
}

/**
 * Shows [content] when [open], fading and sliding it in/out (no offscreen copy). The content stays composed
 * while closed, so opening is instant; closed, it's measured but not placed, which also keeps it out of
 * D-pad focus search.
 */
@Composable
private fun KeptPanel(open: Boolean, content: @Composable () -> Unit) {
    val progress = androidx.compose.animation.core.animateFloatAsState(
        if (open) 1f else 0f, tween(if (open) 200 else 120), label = "veil-panel",
    )
    val placed by remember { androidx.compose.runtime.derivedStateOf { progress.value > 0f } }
    val show = open || placed
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = -24.dp.toPx() * (1f - p)
            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val w = placeables.maxOfOrNull { it.width } ?: 0
        val h = placeables.maxOfOrNull { it.height } ?: 0
        layout(w, h) { if (show) placeables.forEach { it.place(0, 0) } }
    }
}

@Composable
private fun VeilPanel(state: LauncherState) {
    var focusedTheme by remember { mutableStateOf<VeilThemeSpec?>(null) }
    val current = ThemeState.current
    val vizButton = remember { FocusRequester() }
    val weatherButton = remember { FocusRequester() }
        val shortcutsButton = remember { FocusRequester() }
    // The panel stays built between opens; start each open scrolled to the top, as before.
    val scroll = rememberScrollState()
    LaunchedEffect(state.veilMenuOpen) { if (!state.veilMenuOpen) scroll.scrollTo(0) }
    // After a window opened from this menu closes, put focus back on the button that opened it.
    LaunchedEffect(state.menuReturn) {
                val target = when (state.menuReturn) { "viz" -> vizButton; "weather" -> weatherButton; "shortcuts" -> shortcutsButton; else -> null }
        if (target != null) {
            delay(80)
            runCatching { target.requestFocus() }
            state.menuReturn = null
        }
    }
    Column(
        Modifier
            .onPreviewKeyEvent { e ->
                // Back closes the menu by sending focus back to the page.
                if (e.key == Key.Back && e.type == KeyEventType.KeyUp) { Sfx.back(); state.focusHome(); true }
                else e.key == Key.Back
            }
            .width(330.dp).heightIn(max = 445.dp).clip(RoundedCornerShape(20.dp)).background(Veil.Sheet)
            .border(1.dp, Veil.Line, RoundedCornerShape(20.dp))
            .verticalScroll(scroll) // focus moving down scrolls it into view
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("veil", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp)
            Text("Private by design · no ads · no tracking", fontSize = 9.sp, color = Veil.Text.copy(alpha = 0.6f))
        }

        // Ambient
        MenuButton(icon = VeilIcons.Sparkle, title = "Start Veil Ambient", subtitle = "Screensaver & music mode", onClick = state::startAmbient)

        // Music mode: what Veil Ambient shows while music plays
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("MUSIC MODE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("Veil", VeilIcons.Note, state.musicStyle == "veil", Modifier.weight(1f)) { state.chooseMusicStyle("veil") }
                ModeChip(
                    when {
                        state.visualizerAllowed -> "Visualizer"
                        !state.visualizerSupported -> "Visualizer (not supported on this TV)"
                        else -> "Visualizer (no audio access)"
                    },
                    VeilIcons.Wave, state.musicStyle == "viz", Modifier.weight(1f),
                ) { state.chooseMusicStyle("viz") }
            }
            if (state.musicStyle == "veil") {
                MenuButton(
                    icon = VeilIcons.Lyrics,
                    title = "Lyrics: " + if (state.lyricsMusic) "on" else "off",
                    subtitle = "Synced lyrics next to the album art",
                    onClick = state::toggleLyricsMusic,
                )
            }
            if (state.musicStyle == "viz" && state.visualizerAllowed) {
                MenuButton(
                    icon = VeilIcons.Wave, title = "Visualizer settings", subtitle = "Sensitivity, motion, trails… with live preview",
                    modifier = Modifier.focusRequester(vizButton),
                    onClick = { state.vizSettingsOpen = true },
                )
            }
        }

        // Themes
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val shown = focusedTheme ?: current
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("THEME")
                Spacer(Modifier.width(8.dp))
                Text(shown.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = shown.accent)
                if (shown.id == current.id) {
                    Spacer(Modifier.width(5.dp))
                    Text("· active", fontSize = 9.sp, color = Veil.Text.copy(alpha = 0.5f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Themes.all.forEach { t ->
                    ThemeBubble(
                        theme = t,
                        selected = t.id == current.id,
                        onClick = { state.setTheme(t) },
                        onFocus = { focusedTheme = if (it) t else if (focusedTheme == t) null else focusedTheme },
                    )
                }
            }
            MenuButton(
                icon = VeilIcons.Sparkle,
                title = "Colours from artwork: " + if (state.artColors) "on" else "off",
                subtitle = "The accent glows in the colours of the show you're on",
                onClick = state::toggleArtColors,
            )
        }

        // Weather location
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("WEATHER")
            val w = state.weather
            MenuButton(
                icon = if (w != null) weatherIcon(w) else VeilIcons.Pin2,
                title = state.weatherPlace ?: "No location set",
                subtitle = (w?.let { "${it.tempC}° now · " } ?: "") + "Change location",
                modifier = Modifier.focusRequester(weatherButton),
                onClick = { state.weatherPicker = true },
            )
        }

        // Seasonal touches
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("SOUNDS & SEASONAL")
            MenuButton(
                icon = VeilIcons.Note,
                title = "Interface sounds: " + if (state.sounds) "on" else "off",
                subtitle = "Soft ticks and chimes as you move around",
                onClick = state::toggleSounds,
            )
            MenuButton(
                icon = if (Seasons.isWinter()) VeilIcons.Snowflake else VeilIcons.Sparkle,
                title = "Seasonal touches: " + if (state.seasonal) "on" else "off",
                subtitle = "Snow in December, New Year countdown… in Ambient",
                onClick = state::toggleSeasonal,
            )
        }
        // Shortcuts
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("SHORTCUTS")
            MenuButton(
                icon = VeilIcons.Sparkle,
                title = "Start app & button shortcuts",
                subtitle = "Open an app on startup or with a remote button",
                modifier = Modifier.focusRequester(shortcutsButton),
                onClick = { ShortcutsUi.open = true },
            )
        }
        Text("Back to close", fontSize = 8.5.sp, color = Veil.Text.copy(alpha = 0.45f))
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Veil.Text.copy(alpha = 0.55f))

@Composable
private fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { Sfx.select(); onClick() },
        modifier = modifier.fillMaxWidth().focusSound(1.12f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.06f), contentColor = Veil.Text,
            focusedContainerColor = Veil.Text, focusedContentColor = Veil.Ink,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val onWhite = LocalContentColor.current == Veil.Ink
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (onWhite) Veil.Ink.copy(alpha = 0.08f) else Veil.Accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (onWhite) Veil.Ink else Veil.Accent)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 9.sp, color = dimColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** One option of a two-way switch: selected = accent outline + check. */
@Composable
private fun ModeChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = { Sfx.select(); onClick() },
        modifier = modifier.focusSound(1.12f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Veil.Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f),
            contentColor = Veil.Text,
            focusedContainerColor = Veil.Text, focusedContentColor = Veil.Ink,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(1.5.dp, Veil.Accent), shape = RoundedCornerShape(50)) else Border.None,
            focusedBorder = Border.None,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val onWhite = LocalContentColor.current == Veil.Ink
            Icon(
                if (selected) VeilIcons.Check else icon, contentDescription = null, modifier = Modifier.size(12.dp),
                tint = if (onWhite) Veil.Ink else if (selected) Veil.Accent else Veil.Text,
            )
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A round theme preview: the theme's background, an accent glow, and its glyph. */
@Composable
private fun ThemeBubble(theme: VeilThemeSpec, selected: Boolean, onClick: () -> Unit, onFocus: (Boolean) -> Unit) {
    Surface(
        onClick = { Sfx.select(); onClick() },
        modifier = Modifier.size(32.dp).onFocusChanged { onFocus(it.isFocused) }.focusSound(1.25f),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = theme.bg, focusedContainerColor = theme.bg),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(2.dp, theme.accent), shape = CircleShape)
            else Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), shape = CircleShape),
            focusedBorder = Border(BorderStroke(2.5.dp, Color.White), shape = CircleShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.22f),
    ) {
        Box(
            Modifier.size(32.dp).background(
                Brush.radialGradient(listOf(theme.accent.copy(alpha = 0.45f), Color.Transparent), radius = 40f),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(theme.glyph(), contentDescription = theme.name, modifier = Modifier.size(14.dp), tint = theme.accent)
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(11.dp).clip(CircleShape).background(theme.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VeilIcons.Check, contentDescription = "Active", modifier = Modifier.size(8.dp), tint = Veil.Ink)
            }
        }
    }
}

/** Weather location search: the TV keyboard (with voice) opens right away; results update as you type. */
@Composable
fun WeatherPicker(state: LauncherState) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Weather.Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val close = { state.weatherPicker = false; state.menuReturn = "weather" }

    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(350)
        searching = true
        results = withContext(Dispatchers.IO) { Weather.search(query) }
        searching = false
    }

    Dialog(onDismissRequest = { Sfx.back(); close() }) {
        Column(
            Modifier.width(320.dp).clip(RoundedCornerShape(20.dp)).background(Veil.Sheet)
                .border(1.dp, Veil.Line, RoundedCornerShape(20.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Weather location", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Now: ${state.weatherPlace ?: "not set"}", fontSize = 9.5.sp, color = Veil.Text.copy(alpha = 0.6f))
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Veil.Text, fontSize = 13.sp, fontFamily = Manrope),
                cursorBrush = SolidColor(Veil.Accent),
                modifier = Modifier.fillMaxWidth().focusRequester(field)
                    .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Veil.Line, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(VeilIcons.Search, contentDescription = null, modifier = Modifier.size(13.dp), tint = Veil.Text.copy(alpha = 0.6f))
                        Spacer(Modifier.width(8.dp))
                        Box {
                            if (query.isEmpty()) Text("Type or say a city…", fontSize = 13.sp, color = Veil.Text.copy(alpha = 0.45f))
                            inner()
                        }
                    }
                },
            )
            if (searching) Text("Searching…", fontSize = 9.5.sp, color = Veil.Text.copy(alpha = 0.6f))
            results.forEach { p ->
                PickerRow(title = p.name, subtitle = p.region) { state.setWeatherPlace(p); close() }
            }
            if (state.weatherHasOverride && state.weatherDefault.isNotEmpty()) {
                PickerRow(title = "Use default", subtitle = state.weatherDefault) { state.resetWeatherPlace(); close() }
            }
            Text("Back to cancel", fontSize = 8.5.sp, color = Veil.Text.copy(alpha = 0.45f))
        }
    }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { field.requestFocus() }
    }
}

@Composable
private fun PickerRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = { Sfx.select(); onClick() },
        modifier = Modifier.fillMaxWidth().focusSound(1.12f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, contentColor = Veil.Text,
            focusedContainerColor = Veil.Text, focusedContentColor = Veil.Ink,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(VeilIcons.Pin2, contentDescription = null, modifier = Modifier.size(12.dp), tint = LocalContentColor.current)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Text(subtitle, fontSize = 9.5.sp, color = dimColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
