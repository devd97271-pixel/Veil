package dev.veil.launcher

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent

/** Saved choices: the app to open when the TV starts, and which remote button opens which app. */
object Shortcuts {
    /** Remote buttons that can be mapped, in the order they are shown in the menu. */
    val KEYS: List<Pair<Int, String>> = listOf(
        KeyEvent.KEYCODE_1 to "Button 1",
        KeyEvent.KEYCODE_2 to "Button 2",
        KeyEvent.KEYCODE_3 to "Button 3",
        KeyEvent.KEYCODE_4 to "Button 4",
        KeyEvent.KEYCODE_5 to "Button 5",
        KeyEvent.KEYCODE_6 to "Button 6",
        KeyEvent.KEYCODE_7 to "Button 7",
        KeyEvent.KEYCODE_8 to "Button 8",
        KeyEvent.KEYCODE_9 to "Button 9",
        KeyEvent.KEYCODE_0 to "Button 0",
        KeyEvent.KEYCODE_PROG_RED to "Red button",
        KeyEvent.KEYCODE_PROG_GREEN to "Green button",
        KeyEvent.KEYCODE_PROG_YELLOW to "Yellow button",
        KeyEvent.KEYCODE_PROG_BLUE to "Blue button",
    )

    private fun prefs(c: Context) = c.getSharedPreferences("veil_shortcuts", Context.MODE_PRIVATE)

    fun startApp(c: Context): String? = prefs(c).getString("start_app", null)

    fun setStartApp(c: Context, pkg: String?) {
        val e = prefs(c).edit()
        if (pkg == null) e.remove("start_app") else e.putString("start_app", pkg)
        e.apply()
    }

    fun keyApp(c: Context, keyCode: Int): String? = prefs(c).getString("key_$keyCode", null)

    fun setKeyApp(c: Context, keyCode: Int, pkg: String?) {
        val e = prefs(c).edit()
        if (pkg == null) e.remove("key_$keyCode") else e.putString("key_$keyCode", pkg)
        e.apply()
    }

    /** True only the first time Veil starts after the TV was restarted. */
    fun isFirstRunThisBoot(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val boot = Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, -1)
        if (boot < 0) return false
        val p = prefs(c)
        if (p.getInt("last_boot_count", -1) == boot) return false
        p.edit().putInt("last_boot_count", boot).apply()
        return true
    }
}
