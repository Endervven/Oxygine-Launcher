package dev.endervven.oxygenlauncher

import android.content.Context

object LauncherPrefs {
    private const val FILE = "oxygen_launcher_prefs"
    private const val KEY_ACCENT = "accent"
    private const val KEY_DARK = "dark"
    private const val KEY_LABELS = "labels"
    private const val KEY_GRID = "grid"
    private const val KEY_ANIM = "animations"
    private const val KEY_GESTURE = "gesture"

    fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun accent(context: Context) = prefs(context).getInt(KEY_ACCENT, 0xFF4778F5.toInt())
    fun dark(context: Context) = prefs(context).getBoolean(KEY_DARK, false)
    fun labels(context: Context) = prefs(context).getBoolean(KEY_LABELS, true)
    fun grid(context: Context) = prefs(context).getInt(KEY_GRID, 4)
    fun animations(context: Context) = prefs(context).getBoolean(KEY_ANIM, true)
    fun gesture(context: Context) = prefs(context).getString(KEY_GESTURE, "drawer") ?: "drawer"
}
