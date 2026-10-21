package dev.endervven.oxygenlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import android.app.AlertDialog

class SettingsActivity : Activity() {
    private val dark get() = LauncherPrefs.dark(this)
    private val bg get() = if (dark) Color.rgb(17, 19, 23) else Color.rgb(247, 248, 251)
    private val surface get() = if (dark) Color.rgb(29, 32, 38) else Color.WHITE
    private val fg get() = if (dark) Color.rgb(245, 246, 250) else Color.rgb(26, 29, 36)
    private val secondary get() = if (dark) Color.rgb(164, 170, 182) else Color.rgb(108, 115, 128)

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun rounded(c: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(20).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = if (!dark) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0

        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }
        scroll.addView(page)
        setContentView(scroll)

        page.addView(TextView(this).apply {
            text = "Launcher Studio"
            textSize = 29f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(fg)
        })
        page.addView(TextView(this).apply {
            text = "Gestalte deinen Homescreen."
            textSize = 14f; setTextColor(secondary); setPadding(0, dp(4), 0, dp(20))
        })

        section(page, "Erscheinungsbild")
        addSwitch(page, "Dunkles Design", "Dunkle Oberflächen für abends", dark) { value ->
            LauncherPrefs.prefs(this).edit().putBoolean("dark", value).apply(); recreate()
        }
        addSwitch(page, "App-Namen anzeigen", "Labels unter den Icons", LauncherPrefs.labels(this)) { value ->
            LauncherPrefs.prefs(this).edit().putBoolean("labels", value).apply()
        }
        val colors = listOf(
            "Oxygen Blue" to 0xFF4778F5.toInt(),
            "Mint" to 0xFF1DAD91.toInt(),
            "Violet" to 0xFF8A63E6.toInt(),
            "Sunset" to 0xFFE77756.toInt(),
            "Graphite" to 0xFF657080.toInt()
        )
        addChoice(page, "Akzentfarbe", "Farbe für Bedienelemente", colors.firstOrNull { it.second == LauncherPrefs.accent(this) }?.first ?: "Oxygen Blue") {
            AlertDialog.Builder(this).setTitle("Akzentfarbe")
                .setItems(colors.map { it.first }.toTypedArray()) { _, which ->
                    LauncherPrefs.prefs(this).edit().putInt("accent", colors[which].second).apply()
                    recreate()
                }.show()
        }
        val grids = listOf("3 Spalten", "4 Spalten", "5 Spalten", "6 Spalten")
        addChoice(page, "Icon-Raster", "Anzahl der Icons pro Reihe", "${LauncherPrefs.grid(this)} Spalten") {
            AlertDialog.Builder(this).setTitle("Icon-Raster").setItems(grids.toTypedArray()) { _, which ->
                LauncherPrefs.prefs(this).edit().putInt("grid", which + 3).apply()
            }.show()
        }

        section(page, "Animationen & Verhalten")
        addSwitch(page, "Flüssige Animationen", "Übergänge und Icon-Feedback", LauncherPrefs.animations(this)) { value ->
            LauncherPrefs.prefs(this).edit().putBoolean("animations", value).apply()
        }
        addChoice(page, "Wischgeste", "Aktion auf dem Homescreen", if (LauncherPrefs.gesture(this) == "drawer") "App-Drawer öffnen" else "Keine Aktion") {
            val options = arrayOf("App-Drawer öffnen", "Keine Aktion")
            AlertDialog.Builder(this).setTitle("Wischgeste").setItems(options) { _, which ->
                LauncherPrefs.prefs(this).edit().putString("gesture", if (which == 0) "drawer" else "none").apply()
            }.show()
        }

        section(page, "System")
        addChoice(page, "Standard-Launcher", "Android-Start-App auswählen", "Systemeinstellungen öffnen") {
            try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
        addChoice(page, "Hintergrundbild", "System-Wallpaper ändern", "Öffnen") {
            startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
        }
        addChoice(page, "App-Info", "Version 1.0 · Oxygen Launcher", "Berechtigungen & Details") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
        }
        val footer = TextView(this).apply {
            text = "OXYGEN LAUNCHER  •  BUILT FOR YOUR FLOW"
            textSize = 10f; letterSpacing = .12f; setTextColor(secondary)
            gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(8))
        }
        page.addView(footer)
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(secondary)
            setPadding(dp(2), dp(18), 0, dp(8))
        })
    }

    private fun addSwitch(parent: LinearLayout, title: String, desc: String, checked: Boolean, change: (Boolean) -> Unit) {
        val row = rowCard(parent, title, desc)
        val toggle = Switch(this).apply { isChecked = checked; buttonTintList = android.content.res.ColorStateList.valueOf(LauncherPrefs.accent(this@SettingsActivity)) }
        row.addView(toggle)
        toggle.setOnCheckedChangeListener { _, isChecked -> change(isChecked) }
    }

    private fun addChoice(parent: LinearLayout, title: String, desc: String, value: String, click: () -> Unit) {
        val row = rowCard(parent, title, desc)
        row.addView(TextView(this).apply {
            text = value; textSize = 12f; setTextColor(LauncherPrefs.accent(this@SettingsActivity))
            gravity = Gravity.CENTER_VERTICAL; maxWidth = dp(125)
        })
        row.setOnClickListener { click() }
    }

    private fun rowCard(parent: LinearLayout, title: String, desc: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(surface); setPadding(dp(15), dp(12), dp(14), dp(12))
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(TextView(this).apply { text = title; textSize = 15f; setTextColor(fg); typeface = Typeface.DEFAULT_BOLD })
        labels.addView(TextView(this).apply { text = desc; textSize = 12f; setTextColor(secondary); setPadding(0, dp(3), dp(6), 0) })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        return row
    }
}
